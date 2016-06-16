package com.theironyard;

import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import org.h2.tools.Server;
import spark.Spark;

import java.sql.*;
import java.util.ArrayList;

public class Main {

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, username VARCHAR, address VARCHAR, email VARCHAR)");
    }

    public static void insertUsers(Connection conn, String username, String address, String email) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, address);
        stmt.setString(3, email);
        stmt.execute();
    }

    public static ArrayList<User> selectUsers(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
        ResultSet results = stmt.executeQuery();
        ArrayList<User> users = new ArrayList<>();
        while (results.next()) {
            Integer id = results.getInt("id");
            String username = results.getString("username");
            String address = results.getString("address");
            String email = results.getString("email");
            User user = new User(id, username, address, email);
            users.add(user);
        }
        return users;
    }

    public static void updateUser(Connection conn, User reg) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE registrations SET username=?, address=?, email=? WHERE id=?");
        stmt.setString(1, reg.username);
        stmt.setString(2, reg.address);
        stmt.setString(3, reg.email);
        stmt.setInt(4, reg.id);
        stmt.execute();
    }

    public static void deleteUser(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM registrations WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        Spark.externalStaticFileLocation("public");
        Spark.init();

        Spark.get(
                "/user",
                (request, response) -> {
                    ArrayList<User> regs = selectUsers(conn);
                    JsonSerializer s = new JsonSerializer();
                    return s.serialize(regs);
                }
        );

        Spark.post(
                "/user",
                (request, response) -> {
                    String body = request.body();
                    JsonParser p = new JsonParser();
                    User reg = p.parse(body, User.class);
                    insertUsers(conn, reg.username, reg.address, reg.email);
                    return "";
                }
        );

        Spark.put(
                "/user",
                (request, response) -> {
                    String body = request.body();
                    JsonParser p = new JsonParser();
                    User reg = p.parse(body, User.class);
                    updateUser(conn, reg);
                    return "";
                }
        );

        Spark.delete(
                "/user/:id",
                (request, response) -> {
                    int id = Integer.valueOf(request.params("id"));
                    deleteUser(conn, id);
                    return"";
                }
        );


    }
}
