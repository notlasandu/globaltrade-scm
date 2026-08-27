package com.globaltrade.ejb;

import com.globaltrade.core.entity.Inventory;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

@Stateless
@PermitAll
public class InventoryManagerBean implements InventoryManagerLocal, InventoryManagerRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Inventory> getAvailableProducts() {
        TypedQuery<Inventory> query = entityManager.createQuery(
                "SELECT i FROM Inventory i LEFT JOIN FETCH i.primaryVendor WHERE i.quantity > 0", Inventory.class);
        return new java.util.ArrayList<>(query.getResultList());
    }
}
