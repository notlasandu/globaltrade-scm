-- This file is automatically executed by Hibernate after schema generation
-- Insert a dummy hospital customer so our HospitalActor client can log in and place an order!

INSERT INTO Customer (customerId, hospitalName, contactEmail, loginUsername, loginPasswordHash) VALUES (1, 'City General Hospital', 'admin@cityhospital.com', 'hospitaladmin', 'dummyhash');

INSERT INTO vendors (id, name, contactEmail, performanceRating) VALUES (1, 'MedTech Supplies', 'sales@medtech.com', 'A+');
INSERT INTO vendors (id, name, contactEmail, performanceRating) VALUES (2, 'Global Devices Inc', 'orders@globaldevices.com', 'A');

INSERT INTO inventory (sku, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('Surgical Masks', 10000, 'Warehouse A', 2000, 5000, 1);
INSERT INTO inventory (sku, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('MRI Machine', 2, 'Warehouse B', 1, 2, 2);
INSERT INTO inventory (sku, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('Antibiotics', 500, 'Warehouse A', 100, 500, 1);
INSERT INTO inventory (sku, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('Defibrillator', 15, 'Warehouse C', 5, 10, 2);
