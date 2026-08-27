<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Customer Portal - Login</title>
</head>
<body>
    <h2>GlobalTrade Customer Portal</h2>
    <form method="POST" action="j_security_check">
        <div>
            <label for="j_username">Username:</label>
            <input type="text" id="j_username" name="j_username" required>
        </div>
        <div>
            <label for="j_password">Password:</label>
            <input type="password" id="j_password" name="j_password" required>
        </div>
        <div>
            <button type="submit">Login</button>
        </div>
    </form>
</body>
</html>
