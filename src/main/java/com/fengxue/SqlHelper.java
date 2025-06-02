package com.fengxue;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlHelper {
    // 数据库连接信息
    private static final String DB_PATH = System.getenv("LOCALAPPDATA")
            + File.separator + "fengxue"
            + File.separator + "note"
            + File.separator + "notedb";
    private static final String JDBC_URL = "jdbc:h2:" + DB_PATH + ";AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "thisisasecret";
    private Connection connection;

    public SqlHelper() {
        connect();
        initTables();
    }

    // 连接数据库
    private void connect() {
        try {
            // 确保目录存在
            File dbDir = new File(DB_PATH).getParentFile();
            if (!dbDir.exists()) { dbDir.mkdirs();}
            connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    // 初始化数据表
    private void initTables() {
        String createConfigTable = "CREATE TABLE IF NOT EXISTS config ("+
                                    "id INT AUTO_INCREMENT PRIMARY KEY,"+
                                    "myname TEXT,"+
                                    "myvalue TEXT)";

        String createDataTable = "CREATE TABLE IF NOT EXISTS lable ("+
                                "id INT AUTO_INCREMENT PRIMARY KEY,"+
                                "myname TEXT,"+
                                "myvalue TEXT)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createConfigTable);
            stmt.execute(createDataTable);
            if (isTableEmpty("config")) { insertInitialConfigs();}
        } catch (SQLException e) {
            throw new RuntimeException("创建表失败", e);
        }
    }
    // 检查表是否为空
    private boolean isTableEmpty(String table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }
    // 插入基础配置项
    private void insertInitialConfigs() {
        String[] initialKeys = {"是否置顶", "是否锁定","是否显示","鼠标穿透","横","纵","高","宽","透明度","热键主","热键副","自动保存时间"};
        String[] initialValues = {"1", "0","1","0","0","0","500","250","0.7","Alt","Q","0"};

        String sql = "INSERT INTO config (myname, myvalue) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < initialKeys.length; i++) {
                pstmt.setString(1, initialKeys[i]);
                pstmt.setString(2, initialValues[i]);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("插入基础配置失败", e);
        }
    }
    // 通用查询方法
    public List<Map<String, String>> queryForList(String table, String nm) {
        String sql = "SELECT * FROM " + table + " WHERE " + " myname" + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nm);
            ResultSet rs = pstmt.executeQuery();

            List<Map<String, String>> result = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id", String.valueOf(rs.getInt("id")));
                row.put("myname", rs.getString("myname"));
                row.put("myvalue", rs.getString("myvalue"));
                result.add(row);
            }

            return result;

        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }
    //查询lable表所有数据，返回对应的map
    public Map<String, String> lableget() {
        String sql = "SELECT * FROM lable";
        try (Statement stmt = connection.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            Map<String, String> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("myname"), rs.getString("myvalue"));
            }
            return map;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return new HashMap<>();

    }

    //查询config表，返回对应的map
    public Map<String, String> configget() {
        String sql = "SELECT * FROM config";
        try (Statement stmt = connection.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            Map<String, String> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("myname"), rs.getString("myvalue"));
            }
            return map;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return new HashMap<>();
    }
    // 通用插入方法
    public int insert(String table, String key, String value) {
        String sql = "INSERT INTO " + table + " (myname, myvalue) VALUES (?, ?)"; // 修复字段名
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            return -1;
        }
        return -1;
    }

    // 通用更新方法
    public boolean update(String table,  String key, String value) {
        String sql = "UPDATE " + table + " SET myvalue = ? WHERE myname = ?"; // 修复字段名
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, value);
            pstmt.setString(2, key);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // 通用删除方法（添加表名校验）
    public boolean delete(String table, String name) {
        String sql = "DELETE FROM " + table + " WHERE myname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    // 关闭连接
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("关闭连接失败", e);
        }
    }

    // 获取数据库连接（用于复杂查询）
    public Connection getConnection() {
        return connection;
    }
}

