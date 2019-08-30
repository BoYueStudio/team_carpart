<%--
  Created by IntelliJ IDEA.
  User: BoYue
  Date: 2019/8/27
  Time: 16:11
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<form action="${ctx}/login/ossTest" method="post" enctype="multipart/form-data">

    上传文件:<input type="file" name="file"/><br><br>
    <button type="submit" value="上传">上传</button>
</form>

</body>
</html>
