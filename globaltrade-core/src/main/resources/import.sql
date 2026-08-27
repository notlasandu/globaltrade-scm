-- This file is automatically executed by Hibernate after schema generation
-- Insert a dummy hospital customer so our HospitalActor client can log in and place an order!

INSERT INTO Customer (customerId, hospitalName, contactEmail, loginUsername, loginPasswordHash) VALUES (1, 'City General Hospital', 'admin@cityhospital.com', 'hospitaladmin', 'dummyhash');

INSERT INTO inventory (sku, quantity, location) VALUES ('Surgical Masks', 10000, 'Warehouse A');
INSERT INTO inventory (sku, quantity, location) VALUES ('MRI Machine', 2, 'Warehouse B');
INSERT INTO inventory (sku, quantity, location) VALUES ('Antibiotics', 500, 'Warehouse A');
INSERT INTO inventory (sku, quantity, location) VALUES ('Defibrillator', 15, 'Warehouse C');
