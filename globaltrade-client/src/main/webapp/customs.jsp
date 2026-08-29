<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.globaltrade.core.entity.Shipment" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Government Customs Terminal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/shadcn-base.css">
    <script src="${pageContext.request.contextPath}/scripts/theme-switch.js"></script>
    <style>
        .dashboard-container { max-width: 1000px; margin: 0 auto; padding: 2rem; }
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
        <h1 class="text-2xl font-semibold mb-6">Government Customs Clearance Terminal</h1>

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

        <div class="card p-6">
            <h2 class="text-xl font-semibold mb-4">Pending Shipments Awaiting Clearance</h2>
            <div style="overflow-x: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>Shipment ID</th>
                            <th>Tracking Number</th>
                            <th>Vendor</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% 
                        List<Shipment> pending = (List<Shipment>) request.getAttribute("pending");
                        if (pending != null && !pending.isEmpty()) {
                            for (Shipment s : pending) {
                                String vendorName = s.getVendor() != null ? s.getVendor().getName() : "Unknown";
                    %>
                        <tr>
                            <td>#<%= s.getId() %></td>
                            <td><%= s.getTrackingNumber() %></td>
                            <td><%= vendorName %></td>
                            <td>
                                <form action="${pageContext.request.contextPath}/dashboard/customs" method="POST" class="flex gap-2" style="margin:0;">
                                    <input type="hidden" name="shipmentId" value="<%= s.getId() %>">
                                    <button type="submit" name="action" value="approve" class="btn text-sm" style="height:2rem; background-color: #22c55e; color: white;">Approve</button>
                                    <button type="submit" name="action" value="reject" class="btn text-sm" style="height:2rem; background-color: #ef4444; color: white;">Reject</button>
                                </form>
                            </td>
                        </tr>
                    <% 
                            }
                        } else {
                    %>
                        <tr><td colspan="4">No shipments currently waiting for clearance.</td></tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
</html>
