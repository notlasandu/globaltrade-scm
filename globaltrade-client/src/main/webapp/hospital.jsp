<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.globaltrade.core.entity.Inventory" %>
<%@ page import="com.globaltrade.core.entity.Order" %>
<%@ page import="com.globaltrade.core.entity.OrderItem" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Hospital Ordering Portal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/shadcn-base.css">
    <script src="${pageContext.request.contextPath}/scripts/theme-switch.js"></script>
    <style>
        .dashboard-container { max-width: 1200px; margin: 0 auto; padding: 2rem; }
        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
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
        <h1 class="text-2xl font-semibold mb-6">Hospital Ordering Portal</h1>

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
                <h2 class="text-xl font-semibold mb-4">Available Inventory</h2>
                <form action="${pageContext.request.contextPath}/dashboard/hospital" method="POST" class="flex gap-2 mb-6 items-center">
                    <input type="hidden" name="action" value="order">
                    <input type="text" name="sku" placeholder="SKU" class="input" required style="width: 150px;">
                    <input type="number" name="qty" placeholder="Qty" class="input" required style="width: 100px;" min="1">
                    <button type="submit" class="btn">Place Order</button>
                </form>
                
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>SKU</th>
                                <th>Product</th>
                                <th>Quantity</th>
                                <th>Location</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<Inventory> products = (List<Inventory>) request.getAttribute("products");
                            if (products != null) {
                                for (Inventory p : products) {
                        %>
                            <tr>
                                <td><%= p.getSku() %></td>
                                <td><%= p.getProductName() %></td>
                                <td><%= p.getQuantity() %></td>
                                <td><%= p.getLocation() %></td>
                            </tr>
                        <% 
                                }
                            }
                        %>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Order History</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>Status</th>
                                <th>Items</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<Order> history = (List<Order>) request.getAttribute("history");
                            if (history != null) {
                                for (Order o : history) {
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
                            </tr>
                        <% 
                                }
                            }
                        %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
