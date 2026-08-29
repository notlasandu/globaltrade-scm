<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.globaltrade.core.entity.Order" %>
<%@ page import="com.globaltrade.core.entity.OrderItem" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Warehouse Management Terminal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/shadcn-base.css">
    <script src="${pageContext.request.contextPath}/scripts/theme-switch.js"></script>
    <style>
        .dashboard-container { max-width: 1200px; margin: 0 auto; padding: 2rem; }
        .grid { display: grid; grid-template-columns: 2fr 1fr; gap: 2rem; }
        table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
        th, td { text-align: left; padding: 0.75rem; border-bottom: 1px solid var(--border); }
        th { font-weight: 600; color: var(--muted-foreground); }
    </style>
</head>
<body>
    <div style="position: absolute; top: 1rem; right: 1rem; display: flex; gap: 1rem;">
        <span class="text-sm items-center flex">User: ${sessionScope.username}</span>
        <button id="theme-toggle" class="btn btn-secondary text-sm">Dark Mode</button>
        <a href="${pageContext.request.contextPath}/index.jsp" class="btn btn-secondary text-sm">Logout</a>
    </div>

    <div class="dashboard-container">
        <h1 class="text-2xl font-semibold mb-6">Warehouse Management Terminal</h1>

        <% if (session.getAttribute("successMessage") != null) { %>
            <div class="mb-6 p-4 text-sm" style="background-color: #22c55e; color: white; border-radius: var(--radius);">
                <%= session.getAttribute("successMessage") %>
                <% session.removeAttribute("successMessage"); %>
            </div>
        <% } %>
        
        <% if (session.getAttribute("errorMessage") != null) { %>
            <div class="mb-6 p-4 text-sm" style="background-color: var(--destructive); color: var(--destructive-foreground); border-radius: var(--radius);">
                <%= session.getAttribute("errorMessage") %>
                <% session.removeAttribute("errorMessage"); %>
            </div>
        <% } %>

        <div class="grid">
            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Pending Orders</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>Status</th>
                                <th>Items</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<Order> pendingOrders = (List<Order>) request.getAttribute("pendingOrders");
                            if (pendingOrders != null && !pendingOrders.isEmpty()) {
                                for (Order o : pendingOrders) {
                        %>
                            <tr>
                                <td>#<%= o.getOrderId() %></td>
                                <td><%= o.getOrderDeliveryStatus() %></td>
                                <td>
                                    <ul style="margin:0; padding-left: 1rem;">
                                    <% for (OrderItem item : o.getOrderItems()) { %>
                                        <li><%= item.getQuantityRequested() %>x <%= item.getSku() %></li>
                                    <% } %>
                                    </ul>
                                </td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/dashboard/warehouse" method="POST" style="margin:0;">
                                        <input type="hidden" name="action" value="pack">
                                        <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                        <button type="submit" class="btn btn-secondary text-sm" style="height:2rem;">Pack Order</button>
                                    </form>
                                </td>
                            </tr>
                        <% 
                                }
                            } else {
                        %>
                            <tr><td colspan="4">No pending orders currently waiting.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="flex flex-col gap-6">
                <div class="card p-6">
                    <h2 class="text-xl font-semibold mb-4">Cycle Count (WMS)</h2>
                    <form action="${pageContext.request.contextPath}/dashboard/warehouse" method="POST" class="flex flex-col gap-4">
                        <input type="hidden" name="action" value="reconcile">
                        <div>
                            <label class="label">SKU</label>
                            <input type="text" name="sku" class="input" required>
                        </div>
                        <div>
                            <label class="label">Physical Count</label>
                            <input type="number" name="count" class="input" required min="0">
                        </div>
                        <button type="submit" class="btn">Report to WMS</button>
                    </form>
                </div>

                <div class="card p-6">
                    <h2 class="text-xl font-semibold mb-4">WMS Outage Simulator</h2>
                    <form action="${pageContext.request.contextPath}/dashboard/warehouse" method="POST" class="flex flex-col gap-4">
                        <input type="hidden" name="action" value="wms-outage">
                        <div>
                            <label class="label">Status</label>
                            <select name="isOffline" class="input">
                                <option value="true">Offline (Simulate Outage)</option>
                                <option value="false">Online</option>
                            </select>
                        </div>
                        <button type="submit" class="btn btn-secondary">Toggle Status</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
