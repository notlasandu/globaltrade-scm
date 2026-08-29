<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Carrier Logistics Terminal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/shadcn-base.css">
    <script src="${pageContext.request.contextPath}/scripts/theme-switch.js"></script>
    <style>
        .dashboard-container { max-width: 1200px; margin: 0 auto; padding: 2rem; }
        .grid { display: grid; grid-template-columns: 2fr 1fr; gap: 2rem; }
        .list-group { list-style: none; padding: 0; margin: 0; }
        .list-item { padding: 0.75rem; border-bottom: 1px solid var(--border); }
    </style>
</head>
<body>
    <div style="position: absolute; top: 1rem; right: 1rem; display: flex; gap: 1rem;">
        <span class="text-sm items-center flex">User: ${sessionScope.username}</span>
        <button id="theme-toggle" class="btn btn-secondary text-sm">Dark Mode</button>
        <a href="${pageContext.request.contextPath}/index.jsp" class="btn btn-secondary text-sm">Logout</a>
    </div>

    <div class="dashboard-container">
        <h1 class="text-2xl font-semibold mb-6">Carrier Logistics Terminal</h1>

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
                <h2 class="text-xl font-semibold mb-4">Unified Shipping Manifest</h2>
                <ul class="list-group">
                <% 
                    List<String> manifest = (List<String>) request.getAttribute("manifest");
                    if (manifest != null && !manifest.isEmpty()) {
                        for (String line : manifest) {
                %>
                    <li class="list-item"><%= line %></li>
                <% 
                        }
                    } else {
                %>
                    <li class="list-item">No packages currently waiting for transit.</li>
                <% } %>
                </ul>
            </div>

            <div class="card p-6">
                <h2 class="text-xl font-semibold mb-4">Update Transit Status</h2>
                
                <form action="${pageContext.request.contextPath}/dashboard/carrier" method="POST" class="flex flex-col gap-4">
                    <div>
                        <label class="label">Tracking Number</label>
                        <input type="text" name="trackingNumber" class="input" required>
                    </div>
                    
                    <div>
                        <label class="label">Action</label>
                        <select name="action" class="input">
                            <option value="pickup">Mark IN_TRANSIT (Pickup)</option>
                            <option value="deliver">Mark DELIVERED (Drop-off)</option>
                            <option value="breakdown">Trigger BREAKDOWN (Simulate Outage)</option>
                        </select>
                    </div>
                    
                    <button type="submit" class="btn">Update Status</button>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
