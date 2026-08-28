-- This file is automatically executed by Hibernate after schema generation
-- Insert a dummy hospital customer so our HospitalActor client can log in and place an order!

INSERT INTO Customer (customerId, hospitalName, contactEmail, loginUsername, loginPasswordHash) VALUES (1, 'City General Hospital', 'admin@cityhospital.com', 'hospitaladmin', 'dummyhash');

INSERT INTO vendors (id, name, contactEmail, performanceRating, eligible) VALUES (1, 'MedTech Supplies', 'sales@medtech.com', 'A+', true);
INSERT INTO vendors (id, name, contactEmail, performanceRating, eligible) VALUES (2, 'Global Devices Inc', 'orders@globaldevices.com', 'A', true);

INSERT INTO supplier_evaluations (id, vendor_id, evaluationDate, score, remarks) VALUES (1, 1, '2026-08-01 10:00:00', 95, 'Excellent delivery time');
INSERT INTO supplier_evaluations (id, vendor_id, evaluationDate, score, remarks) VALUES (2, 2, '2026-08-01 11:00:00', 85, 'Good, but missed one deadline');

INSERT INTO inventory (sku, productName, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('MSK-001', 'Surgical Masks', 10000, 'Warehouse A', 2000, 5000, 1);
INSERT INTO inventory (sku, productName, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('MRI-001', 'MRI Machine', 2, 'Warehouse B', 1, 2, 2);
INSERT INTO inventory (sku, productName, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('ANT-001', 'Antibiotics', 500, 'Warehouse A', 100, 500, 1);
INSERT INTO inventory (sku, productName, quantity, location, reorderThreshold, reorderQuantity, primaryVendorId) VALUES ('DEF-001', 'Defibrillator', 15, 'Warehouse C', 5, 10, 2);
