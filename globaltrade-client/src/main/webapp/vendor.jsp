<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.globaltrade.core.entity.SupplierEvaluation" %>
<%@ page import="com.globaltrade.core.entity.SupplierOrder" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Vendor Portal</title>
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
        <h1 class="text-2xl font-semibold mb-6">Vendor / Supplier Portal</h1>

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
                <h2 class="text-xl font-semibold mb-4">Requested Restock Orders</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>SKU</th>
                                <th>Qty</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<SupplierOrder> orders = (List<SupplierOrder>) request.getAttribute("orders");
                            if (orders != null && !orders.isEmpty()) {
                                for (SupplierOrder o : orders) {
                        %>
                            <tr>
                                <td>#<%= o.getOrderId() %></td>
                                <td><%= o.getSku() %></td>
                                <td><%= o.getQuantity() %></td>
                                <td><%= o.getStatus() %></td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/dashboard/vendor" method="POST" style="margin:0;" class="flex gap-2 items-center">
                                        <input type="hidden" name="action" value="fulfill">
                                        <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                        
                                        <select name="tradeDocs" class="input" style="width: auto; height: 2rem;">
                                            <option value="true">Docs OK</option>
                                            <option value="false">Missing Docs</option>
                                        </select>
                                        
                                        <input type="text" name="trackingNumber" placeholder="Tracking #" class="input" style="width: 120px; height: 2rem;">
                                        
                                        <button type="submit" class="btn btn-secondary text-sm" style="height:2rem;">Fulfill</button>
                                    </form>
                                </td>
                            </tr>
                        <% 
                                }
                            } else {
                        %>
                            <tr><td colspan="5">No pending orders found.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Performance Evaluations</h2>
                <div style="overflow-x: auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Score</th>
                                <th>Remarks</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% 
                            List<SupplierEvaluation> evaluations = (List<SupplierEvaluation>) request.getAttribute("evaluations");
                            if (evaluations != null && !evaluations.isEmpty()) {
                                for (SupplierEvaluation eval : evaluations) {
                        %>
                            <tr>
                                <td><%= eval.getEvaluationDate() %></td>
                                <td><%= eval.getScore() %>/100</td>
                                <td><%= eval.getRemarks() %></td>
                            </tr>
                        <% 
                                }
                            } else {
                        %>
                            <tr><td colspan="3">No evaluations found.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
