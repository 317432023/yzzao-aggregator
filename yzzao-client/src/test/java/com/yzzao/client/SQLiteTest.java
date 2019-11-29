package com.yzzao.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
/**
 * 本类使用sqlite-jdbc测试sqlite数据的连接入库与读取操作
 * @author Administrator
 *
 */
public class SQLiteTest {
	public static void main(String[] args) throws Exception {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:db/baiyuan.db");
		Statement stmt = conn.createStatement();

		stmt.executeUpdate("DROP TABLE IF EXISTS person");
		stmt.executeUpdate("CREATE TABLE person(id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(255), birth DATETIME DEFAULT (datetime('now','localtime')) )");
		stmt.executeUpdate("INSERT INTO person(name) VALUES('john')");
		stmt.executeUpdate("INSERT INTO person(name) VALUES('david')");
		stmt.executeUpdate("INSERT INTO person(name) VALUES('henry')");
		stmt.executeUpdate("insert into person(name,birth) values('tengj',DATETIME('1984-10-09 06:30:00','+0 hour','-0 minute'))");
		stmt.executeUpdate("insert into person(name,birth) values('tengjson',datetime('1480593328','unixepoch','localtime'))");
		ResultSet rs = stmt.executeQuery("SELECT * FROM person");
		while (rs.next()) {
			System.out.println("id=>" + rs.getInt("id") + ", name=>" + rs.getString("name") + ", birth=>" + rs.getString("birth"));
		}
		stmt.close();
		conn.close();
	}
}