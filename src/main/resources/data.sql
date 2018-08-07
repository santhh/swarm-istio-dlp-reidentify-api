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

