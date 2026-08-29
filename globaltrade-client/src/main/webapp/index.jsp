<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Client Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/shadcn-base.css">
    <script src="${pageContext.request.contextPath}/scripts/theme-switch.js"></script>
    <style>
        .login-container {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 1rem;
        }
        .theme-toggle-wrapper {
            position: absolute;
            top: 1rem;
            right: 1rem;
        }
    </style>
</head>
<body>
    <div class="theme-toggle-wrapper">
        <button id="theme-toggle" class="btn btn-secondary text-sm">Dark Mode</button>
    </div>

    <div class="login-container">
        <div class="card p-6 w-full max-w-md">
            <div class="text-center mb-6">
                <h1 class="text-2xl font-semibold mb-2">Terminal Portal Login</h1>
                <p class="text-sm text-muted-foreground">Select your subsystem and authenticate to connect to the GlobalTrade Logistics engine.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="mb-4 p-4 text-sm" style="background-color: var(--destructive); color: var(--destructive-foreground); border-radius: var(--radius);">
                    <%= request.getAttribute("errorMessage") %>
                </div>
            <% } %>

            <form action="${pageContext.request.contextPath}/login" method="POST" class="flex flex-col gap-4">
                <div>
                    <label for="systemSelect" class="label">System</label>
                    <select id="systemSelect" name="system" class="input" required>
                        <option value="1">Hospital Ordering Portal</option>
                        <option value="2">Warehouse Management Terminal</option>
                        <option value="3">Carrier Logistics Terminal</option>
                        <option value="4">Vendor/Supplier Portal</option>
                        <option value="5">Government Customs Terminal</option>
                    </select>
                </div>
                
                <div>
                    <label for="username" class="label">Username</label>
                    <input type="text" id="username" name="username" class="input" required>
                </div>
                
                <div>
                    <label for="password" class="label">Password</label>
                    <input type="password" id="password" value="password123" name="password" class="input" required>
                </div>
                
                <button type="submit" class="btn w-full mt-2">Connect to Engine</button>
            </form>
        </div>
    </div>

    <script>
        const credentials = {
            "1": { u: "hospitaladmin", p: "password123" },
            "2": { u: "warehousestaff", p: "password123" },
            "3": { u: "carrierdriver", p: "password123" },
            "4": { u: "vendor1", p: "password123" },
            "5": { u: "customsagent", p: "password123" }
        };

        const systemSelect = document.getElementById('systemSelect');
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');

        function updateCredentials() {
            const val = systemSelect.value;
            if (credentials[val]) {
                usernameInput.value = credentials[val].u;
                passwordInput.value = credentials[val].p;
            }
        }

        systemSelect.addEventListener('change', updateCredentials);
        // Initialize on page load
        window.addEventListener('DOMContentLoaded', updateCredentials);
    </script>
</body>
</html>
