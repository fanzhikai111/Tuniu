import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.HtmlPage;

import java.net.URLDecoder;

/*
 * WPsiteCrawler
 * a web crawler for single WordPress site
 * Author:	John Hany
 * Site:	http://johnhany.net/
 * Source code updates:	https://github.com/johnhany/WPCrawler
 * 
 * Using:	Apache HttpComponents 4.3 -- http://hc.apache.org/
 * 			HTML Parser 2.0 -- http://htmlparser.sourceforge.net/
 * 			MySQL Connector/J 5.1.27 -- http://dev.mysql.com/downloads/connector/j/
 * Thanks for their work!
 */

/*
 * @parseFromString
 * extract link from <a> tags in a web page
 */

public class parsePage {
	// 将parser 格式规范化
	public static Parser createParser(String inputHTML) {
		// Lexer mLexer = new Lexer(new Page(inputHTML));
		return new Parser(new Lexer(new Page(inputHTML)),
				new DefaultParserFeedback(DefaultParserFeedback.QUIET));
	}

	public static void parseFromString(String content, Connection conn)
			throws Exception {
		Parser parser = createParser(content);
		parser.setEncoding(parser.getEncoding().equals("ISO-8859-1") ? "gb2312"
				: parser.getEncoding());

		// HtmlPage page = new HtmlPage(parser);
		// System.out.println(page);
		HasAttributeFilter filter1 = new HasAttributeFilter("href");
		// HasAttributeFilter filter2 = new HasAttributeFilter("title");

		try {
			String sql = null;
			ResultSet rs = null;
			PreparedStatement pstmt = null;
			Statement stmt = null;
			NodeList list = parser.parse(filter1);
			int count = list.size();
			ArrayList<String> urls = new ArrayList<String>();// crawled page list
			// process every link on this page
			for (int i = 0; i < count; i++) {
				Node node = list.elementAt(i);
				if (node instanceof LinkTag) {
					LinkTag link = (LinkTag) node;
					String nextlink = link.extractLink();
					String mainurl = ".tuniu.com/";
					String wpurl = "http://www.tuniu.com/";

					// only save page from "http://johnhany.net"
					if (nextlink.contains(mainurl)) {
					

						try {
							// check if the link already exists in the database
							sql = "SELECT * FROM record WHERE URL = '"
									+ nextlink + "'";
							stmt = conn.createStatement(
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_UPDATABLE);
							rs = stmt.executeQuery(sql);
							if (rs.next()) {

							} else {
//								String title = httpGet.StringFilter(link
//										.getLinkText());
								
								if(urls.contains(nextlink)){
									continue;
								}
								else {
									urls.add(nextlink);
								}
								

								// if the link does not exist in the database,
								// insert it
								

								// //use substring for better comparison
								// performance
								// nextlink =
								// nextlink.substring(mainurl.length());
								// //System.out.println(nextlink);

							}
						} catch (SQLException e) {
							// handle the exceptions
							System.out.println("SQLException: "
									+ e.getMessage());
							System.out.println("SQLState: " + e.getSQLState());
							System.out.println("VendorError: "
									+ e.getErrorCode());
						} finally {
							// close and release the resources of
							// PreparedStatement, ResultSet and Statement
							if (pstmt != null) {
								try {
									pstmt.close();
								} catch (SQLException e2) {
								}
							}
							pstmt = null;

							if (rs != null) {
								try {
									rs.close();
								} catch (SQLException e1) {
								}
							}
							rs = null;

							if (stmt != null) {
								try {
									stmt.close();
								} catch (SQLException e3) {
								}
							}
							stmt = null;
						}
						
					}
					
					if (nextlink.contains(mainurl + "tours/")) {
						try {
							// check if the link already exists in the
							// database
					
							sql = "SELECT * FROM tuniu_tours WHERE URL = '"
									+ nextlink + "'";
							stmt = conn.createStatement(
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_UPDATABLE);
							rs = stmt.executeQuery(sql);
							if (rs.next()) {

							} else {
								// if the link does not exist in the
								// database, insert it
								String title = httpGet.StringFilter(link
										.getLinkText());
								sql = "INSERT INTO tuniu_tours (URL,title, begin,destine,duration,price) VALUES ('"
										+ nextlink
										+ "','"
										+ title
										+ "','bg','end',2,3)";
								pstmt = conn.prepareStatement(sql,
										Statement.RETURN_GENERATED_KEYS);
								pstmt.execute();

							}
						} catch (Exception e) {
							// TODO: handle exception
							System.out.println("SQLException: "
									+ e.getMessage());
							System.out.println("SQLState: "
									+ ((SQLException) e).getSQLState());
							System.out.println("VendorError: "
									+ ((SQLException) e).getErrorCode());
						}
					}
					
				}
				
			}
			sql = "INSERT INTO record (URL,crawled) VALUES (?,0)";
			pstmt = conn.prepareStatement(sql,
					Statement.RETURN_GENERATED_KEYS);
			for(String u1:urls){
				pstmt.setString(1, u1);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			conn.commit();
		} catch (ParserException e) {
			e.printStackTrace();
		}
	}
}