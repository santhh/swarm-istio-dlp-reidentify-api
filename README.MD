### DLP Reidentify  API

This repo containts a REST end point to read data from a in memory database and call DLP reidentify API to decruypt tokenized data. This was created to perform load testing with DLP API using Locust

### Sample Data &  DLP Template

There is a CSV file under src/main/resources/import that contains tokenized data.

```
UserId,Password,PhoneNumber,CreditCard,SIN,AccountNumber
dfellowes0@answers.com,dYmeZB,991148-83807,47-81365315879-,48833554028,24601-76780
rkermath1@ox.ac.uk,OKJtHxB,-41220139816,0867032978-2-293566,-1303--8314,92030690208

```

DLP Template used:

DLP Deidentify Template Used:

```
{
 "name": "projects/{id}/deidentifyTemplates/8658110966372436613",
 "createTime": "2018-06-19T15:58:32.214456Z",
 "updateTime": "2018-06-19T15:58:32.214456Z",
 "deidentifyConfig": {
  "recordTransformations": {
   "fieldTransformations": [
    {
     "fields": [
      {
       "name": "SIN"
      },
      {
       "name": "AccountNumber"
      },
      {
       "name": "CreditCard"
      },
      {
       "name": "PhoneNumber"
      }
     ],
     "primitiveTransformation": {
      "cryptoReplaceFfxFpeConfig": {
       "cryptoKey": {
        "kmsWrapped": {
         "wrappedKey": "CiQAku+QvbDmstgYj4NEaoV6FGuB8l3jjWcUiyzJ+HR8NXYZSCASQQBdX/BxUhNiRixvCZnR5/zjFd8D0w9td1w6LUHccIb8HL0s+bK9iOzdllgcXRDC3X9tjx2oqI+S6lFd9tqE5ftd",
         "cryptoKeyName": "projects/{id}/locations/global/keyRings/customer-pii-data-ring/cryptoKeys/pii-data-key"
        }
       },
       "customAlphabet": "1234567890-"
      }
     }
    }
   ]
  }
 }
}

```
### How it works

On startup,  data is loaded in H2 database. Script can be foundsrc/main/resources/data.sql file 

```
DROP TABLE Customers;
CREATE TABLE Customers (
  ID int auto_increment primary key,
  USER_ID VARCHAR(255),
  PASSWORD VARCHAR(1000),
  PHONE_NUMBER VARCHAR(12),
  CREDIT_CARD_NUMBER VARCHAR(50),
  SIN VARCHAR(20),
  ACCT_NUM VARCHAR(100)
) as select null, UserId, Password, PhoneNumber, CreditCard, SIN, AccountNumber from CSVREAD('classpath:import/tokenized_data.csv');

```
if you do a get on following URL, it selects an ID as random and calls DLP reidentify API to detokenized.  

http://localhost:8080/jpa/v1/customers 



If you refreshing the page you will get a random id between 1 to 100,000

```
{
    "id": 89736,
    "userId": "jgiuron1x84@woothemes.com",
    "password": "4YormTT3VU2",
    "phoneNumber": "763-485-3425",
    "creditCardNumber": "5602230422890313",
    "sin": "130-58-9578",
    "acctNum": "828676449-8"
}

```

### Local Build 

After clone the project, you will have to setup two parameters  project.name and template.name.    

Local maven build 

```
mvn clean package 
	

```

Build and push Docker image 

```

docker-credential-gcr configure-docker

mvn compile jib:build -Dproject_id=<project_id> -Dimage_tag=<tag> 
	
docker pull gcr.io/<image_name>

docker run -e "project.name=<id> template.name=projects/<id>/deidentifyTemplates/<id>" <image_id>
	

```

### GKE deployment 

It uses Istio 1.0 with GKE 10+

```
export PROJECT_ID=<id>
export GKE_CLUSTER_NAME=<cluster_name>
export ISTIO_VERSION=1.0.0
export GIT_CLONE_LOCATION=<</Users/masudhasan/Documents/workspace-sts-3.9.3.RELEASE>>
gcloud services enable compute.googleapis.com container.googleapis.com
gcloud beta container --project $PROJECT_ID clusters create $GKE_CLUSTER_NAME --zone "us-central1-a" --username "admin" --cluster-version "1.10.5-gke.2" --machine-type "n1-standard-1" --image-type "COS" --disk-type "pd-standard" --disk-size "100" --scopes "https://www.googleapis.com/auth/cloud-platform" --num-nodes "3" --enable-cloud-logging --enable-cloud-monitoring --network "default" --subnetwork "default" --addons HorizontalPodAutoscaling,HttpLoadBalancing,KubernetesDashboard --enable-autorepair --no-enable-ip-alias
gcloud container clusters get-credentials $GKE_CLUSTER_NAME --zone us-central1-a --project $PROJECT_ID

cd $HOME/Documents/workspace-sts-3.9.3.RELEASE/istio-$ISTIO_VERSION
export PATH=$PWD/bin:$PATH
kubectl create clusterrolebinding cluster-admin-binding \
    --clusterrole=cluster-admin \
    --user=$(gcloud config get-value core/account)

kubectl apply -f install/kubernetes/istio-demo-auth.yaml --as=admin --as-group=system:masters


kubectl get svc -n istio-system
kubectl get pods -n istio-system
kubectl label namespace default istio-injection=enabled
kubectl get namespace -L istio-injection

kubectl apply -f $GIT_CLONE_LOCATION/swarm-istio-dlp-reidentify-api/kubernetes/dlp-reidentify-service.yaml

istioctl create -f $GIT_CLONE_LOCATION/swarm-istio-dlp-reidentify-api/kubernetes/service-ingress.yaml

export INGRESS_HOST=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http")].port}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
curl -o /dev/null -s -w "%{http_code}\n" http://${GATEWAY_URL}/jpa/v1/customers

```

### Load Testing

If you have a Locust cluster running, you can replace the .py file in the locust folder to execute load in the get end point.

```

from locust import HttpLocust, TaskSet, task

class UserBehavior(TaskSet):
    def on_start(self):
        """ on_start is called when a Locust start before any task is scheduled """
        self.index()
    @task(1)
    def index(self):
        self.client.get("/jpa/v1/customers")

class WebsiteUser(HttpLocust):
    task_set = UserBehavior
    min_wait = 5000
    max_wait = 9000
        

```












