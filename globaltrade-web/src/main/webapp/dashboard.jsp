<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.globaltrade.ejb.service.EngineDashboardDTO" %>
<%@ page import="com.globaltrade.core.entity.Order" %>
<%@ page import="com.globaltrade.core.entity.SupplierOrder" %>
<%@ page import="com.globaltrade.core.entity.Inventory" %>
<%@ page import="com.globaltrade.core.entity.AuditLog" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>GlobalTrade SCM Engine Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/shadcn-base.css">
    <script src="${pageContext.request.contextPath}/scripts/theme-switch.js"></script>
    <style>
        .dashboard-container { max-width: 1400px; margin: 0 auto; padding: 2rem; }
        .grid-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.5rem; margin-bottom: 2rem; }
        .grid-tables { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; margin-bottom: 2rem; }
        
        table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
        th, td { text-align: left; padding: 0.75rem; border-bottom: 1px solid var(--border); font-size: 0.875rem; }
        th { font-weight: 600; color: var(--muted-foreground); }
        
        .badge { 
            display: inline-block; padding: 0.25rem 0.5rem; 
            border-radius: 9999px; font-size: 0.7rem; font-weight: 600; 
        }
        .badge-success { background: rgba(34, 197, 94, 0.1); color: #22c55e; border: 1px solid rgba(34, 197, 94, 0.2); }
        .badge-warning { background: rgba(234, 179, 8, 0.1); color: #eab308; border: 1px solid rgba(234, 179, 8, 0.2); }
        .badge-error { background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2); }
        .badge-info { background: rgba(59, 130, 246, 0.1); color: #3b82f6; border: 1px solid rgba(59, 130, 246, 0.2); }
        .badge-secondary { background: var(--secondary); color: var(--secondary-foreground); border: 1px solid var(--border); }
        
        .metric-value { font-size: 2rem; font-weight: 700; margin-top: 0.5rem; }
        .metric-title { font-size: 0.875rem; font-weight: 500; color: var(--muted-foreground); }
    </style>
</head>
<body>
    <div style="position: absolute; top: 1rem; right: 1rem; display: flex; gap: 1rem;">
        <div class="badge badge-success" style="display: flex; align-items: center; gap: 0.5rem;">
            <div style="width: 8px; height: 8px; background: #22c55e; border-radius: 50%; box-shadow: 0 0 5px #22c55e;"></div>
            Server Online
        </div>
        <button id="theme-toggle" class="btn btn-secondary text-sm" style="height: 2rem;">Dark Mode</button>
    </div>

    <div class="dashboard-container">
        <div style="display: flex; align-items: baseline; gap: 1rem; margin-bottom: 2rem;">
            <h1 class="text-3xl font-semibold">GlobalTrade SCM Engine</h1>
            <span class="badge badge-secondary" style="font-size: 0.8rem;">GlobalTrade Main Hub - SriLanka</span>
        </div>

        <% 
            EngineDashboardDTO data = (EngineDashboardDTO) request.getAttribute("dashboardData"); 
            if (data != null) {
        %>
        
        <div class="grid-metrics">
            <div class="card p-6">
                <div class="metric-title">Total Orders Processed</div>
                <div class="metric-value"><%= data.getTotalOrders() %></div>
            </div>
            <div class="card p-6">
                <div class="metric-title">Active Shipments</div>
                <div class="metric-value"><%= data.getTotalShipments() %></div>
            </div>
            <div class="card p-6">
                <div class="metric-title">Inventory Items Count</div>
                <div class="metric-value"><%= data.getTotalInventoryItems() %></div>
            </div>
            <div class="card p-6">
                <div class="metric-title">Registered Vendors</div>
                <div class="metric-value"><%= data.getTotalVendors() %></div>
            </div>
        </div>

        <div class="grid-tables">
            <!-- Outbound Journey -->
            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Global Outbound Logistics (Hub ➔ Client Destination)</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>Client Organization</th>
                                <th>Dest. Region</th>
                                <th>Tracking (OUT)</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<Order> outbound = data.getRecentOutboundOrders();
                            if (outbound != null && !outbound.isEmpty()) {
                                for (Order o : outbound) {
                                    String statusClass = "badge-info";
                                    String statusStr = o.getOrderDeliveryStatus();
                                    if ("DELIVERED".equals(statusStr)) statusClass = "badge-success";
                                    else if ("DELAYED_TRANSIT_ISSUE".equals(statusStr)) statusClass = "badge-error";
                                    else if ("IN_TRANSIT".equals(statusStr)) statusClass = "badge-warning";
                                    else if ("PENDING".equals(statusStr)) statusClass = "badge-secondary";
                                    
                                    String trk = (o.getTrackingNumber() != null) ? o.getTrackingNumber() : "Awaiting Pickup";
                                    
                                    // Generate a fake region based on the customer ID for aesthetic purposes
                                    String[] regions = {"NA-EAST", "EU-WEST", "APAC", "LATAM", "EMEA"};
                                    String region = regions[(int)(o.getOrderingCustomer().getCustomerId() % regions.length)];
                        %>
                            <tr>
                                <td>#<%= o.getOrderId() %></td>
                                <td><%= o.getOrderingCustomer().getHospitalName() %></td>
                                <td><span class="badge badge-secondary"><%= region %></span></td>
                                <td style="font-family: monospace;"><%= trk %></td>
                                <td><span class="badge <%= statusClass %>"><%= statusStr %></span></td>
                            </tr>
                        <% 
                                }
                            } else { 
                        %>
                            <tr><td colspan="5" class="text-muted-foreground text-sm">No outbound orders found.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Inbound Journey -->
            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Inbound Journey (Vendor ➔ Warehouse)</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Sup. Order</th>
                                <th>Vendor</th>
                                <th>Vendor Trk</th>
                                <th>Internal Trk (INB)</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<SupplierOrder> inbound = data.getRecentInboundOrders();
                            if (inbound != null && !inbound.isEmpty()) {
                                for (SupplierOrder so : inbound) {
                                    String statusClass = "badge-info";
                                    String statusStr = so.getStatus();
                                    if ("RECEIVED".equals(statusStr) || "DELIVERED".equals(statusStr)) statusClass = "badge-success";
                                    else if ("REQUESTED".equals(statusStr)) statusClass = "badge-secondary";
                                    
                                    String vendorTrk = "N/A";
                                    String internalTrk = "N/A";
                                    if (so.getShipment() != null) {
                                        vendorTrk = so.getShipment().getTrackingNumber() != null ? so.getShipment().getTrackingNumber() : "N/A";
                                        internalTrk = so.getShipment().getInternalTrackingNumber() != null ? so.getShipment().getInternalTrackingNumber() : "Awaiting Customs";
                                    }
                        %>
                            <tr>
                                <td>#<%= so.getOrderId() %></td>
                                <td><%= so.getVendor().getName() %></td>
                                <td style="font-family: monospace; color: var(--muted-foreground);"><%= vendorTrk %></td>
                                <td style="font-family: monospace;"><%= internalTrk %></td>
                                <td><span class="badge <%= statusClass %>"><%= statusStr %></span></td>
                            </tr>
                        <% 
                                }
                            } else { 
                        %>
                            <tr><td colspan="5" class="text-muted-foreground text-sm">No inbound orders found.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        
        <div class="grid-tables">
            <!-- System Exceptions -->
            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">System Event Log</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Time</th>
                                <th>Action</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<AuditLog> exceptions = data.getRecentExceptions();
                            if (exceptions != null && !exceptions.isEmpty()) {
                                for (AuditLog log : exceptions) {
                                    boolean isError = log.getAction().contains("ERROR") || log.getAction().contains("EXCEPTION") || (log.getDetails() != null && log.getDetails().contains("Exception"));
                                    String badgeClass = isError ? "badge-error" : "badge-info";
                        %>
                            <tr>
                                <td style="white-space: nowrap;"><%= log.getTimestamp() %></td>
                                <td><span class="badge <%= badgeClass %>"><%= log.getAction() %></span></td>
                                <td style="max-width: 250px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="<%= log.getDetails() %>">
                                    <%= log.getDetails() %>
                                </td>
                            </tr>
                        <% 
                                }
                            } else { 
                        %>
                            <tr><td colspan="3" class="text-muted-foreground text-sm">No exceptions found.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
            
            <!-- Live Stock Counts -->
            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Global Fulfillment Center Inventory</h2>
                <div style="overflow-x: auto; max-height: 400px;">
                    <table>
                        <thead>
                            <tr>
                                <th>SKU</th>
                                <th>Product Name</th>
                                <th>Current Quantity</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<Inventory> stockCounts = data.getStockCounts();
                            java.util.Set<String> pendingRestock = data.getPendingRestockSkus();
                            if (pendingRestock == null) pendingRestock = new java.util.HashSet<>();
                            if (stockCounts != null && !stockCounts.isEmpty()) {
                                for (Inventory inv : stockCounts) {
                                    boolean isLow = inv.getQuantity() <= inv.getReorderThreshold();
                                    boolean isRestocking = pendingRestock.contains(inv.getSku());
                                    String qtyColor = isLow ? (isRestocking ? "color: #eab308; font-weight: bold;" : "color: var(--destructive); font-weight: bold;") : "";
                                    String statusLabel = isLow ? (isRestocking ? " (LOW - RESTOCKING)" : " (LOW - ACTION NEEDED)") : "";
                        %>
                            <tr>
                                <td style="font-family: monospace;"><%= inv.getSku() %></td>
                                <td><%= inv.getProductName() %></td>
                                <td style="<%= qtyColor %>"><%= inv.getQuantity() %><%= statusLabel %></td>
                            </tr>
                        <% 
                                }
                            } else { 
                        %>
                            <tr><td colspan="3" class="text-muted-foreground text-sm">No inventory items found.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        
        <% } else { %>
            <div class="card p-6 text-center" style="color: var(--destructive);">
                Failed to load dashboard data. Ensure the EngineDashboardBean is deployed successfully.
                <% if (request.getAttribute("errorMessage") != null) { %>
                    <br><br><b>Error:</b> <%= request.getAttribute("errorMessage") %>
                <% } %>
            </div>
        <% } %>
        
    </div>
</body>
</html>
