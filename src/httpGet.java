import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

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
 * @getByString
 * get page content from an url link
 */
public class httpGet {
	static RequestConfig defaultRequestConfig = RequestConfig.custom()
			.setSocketTimeout(5000).setConnectTimeout(5000)
			.setConnectionRequestTimeout(10000)
			.setStaleConnectionCheckEnabled(true).build();
	static RequestConfig requestConfig = RequestConfig.copy(
			defaultRequestConfig).build();

	// 消除特殊字符
	public static String StringFilter(String str) throws PatternSyntaxException {
		// 只允许字母和数字
		// String regEx = "[^a-zA-Z0-9]";
		// 清除掉所有特殊字符
		String regEx = "[`~!@#$%^&*()+=|{}':;',//[//].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	}
//正常访问标记为真，数据库中标记为1，访问超时标记为假，数据库中标记为-1
	@SuppressWarnings("finally")
	public final static boolean getByString(String url1, Connection conn,
			String title) throws Exception {
boolean bool = true;
		// CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig).build();
		try {
			URL url = new URL(url1);

			URI uri = new URI(url.getProtocol(), url.getHost(), url.getPath(),
					url.getQuery(), null);
			HttpGet httpget = new HttpGet(uri);
			httpget.setConfig(requestConfig);
			System.out.println("executing request " + httpget.getURI());
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity,
								"utf-8") : null;
					} else {
						throw new ClientProtocolException(
								"Unexpected response status: " + status);
					}
				}
			};
			String responseBody = httpclient.execute(httpget, responseHandler);

			Parser parser = parsePage.createParser(responseBody);
			parser.setEncoding(parser.getEncoding().equals("ISO-8859-1") ? "gb2312"
					: parser.getEncoding());

			AndFilter scriptFilter = new AndFilter();
			StringBuilder text = new StringBuilder();
			NodeList nodes = parser.extractAllNodesThatMatch(scriptFilter);
			for (int i = 0; i < nodes.size(); i++) {
				Node node = nodes.elementAt(i);
				if (node instanceof TextNode) {
					// writerTxt(node.getText(), title);

				}
				// text.append(node.getText());
				else {
					continue;
					// text.append('<');
					// text.append(node.getText());
					// text.append('>');
				}
			}
			System.out.println(text.toString());

			// extracLinks(String url,NodeFilter filter);
			// NodeFilter filter_text = new NodeClassFilter(Div.class);
			// parserTheDiv(parser,filter_text);
			// NodeFilter filters = new NodeClassFilter(Div.class);
			//
			// NodeList list1 = parser.extractAllNodesThatMatch(filters);
			// int count1 = list1.size();
			// for(int i=0;i<count1;i++){
			// System.out.println(list1.elementAt(i).toHtml());
			// }
			/*
			 * //print the content of the page
			 * System.out.println("----------------------------------------");
			 * System.out.println(responseBody);
			 * System.out.println("----------------------------------------");
			 */
			parsePage.parseFromString(responseBody, conn);

		}catch (Exception e) {
			// TODO: handle exception
			System.out.println("lianjiechaoshi");
			bool = false;
		} 
		finally {
			httpclient.close();
			
			return bool;
		}
	}

	public static Set<String> extracLinks(String url, NodeFilter filter) {

		Set<String> links = new HashSet<String>();
		try {
			Parser parser = new Parser(url);
			parser.setEncoding(parser.getEncoding().equals("ISO-8859-1") ? "gb2312"
					: parser.getEncoding());
			// 过滤 <frame >标签的 filter，用来提取 frame 标签里的 src 属性所表示的链接
			NodeFilter frameFilter = new NodeFilter() {
				public boolean accept(Node node) {
					if (node.getText().startsWith("frame src=")) {
						return true;
					} else {
						return false;
					}
				}
			};
			// OrFilter 来设置过滤 <a> 标签，和 <frame> 标签
			OrFilter linkFilter = new OrFilter(new NodeClassFilter(
					LinkTag.class), frameFilter);
			// 得到所有经过过滤的标签
			NodeList list = parser.extractAllNodesThatMatch(linkFilter);
			for (int i = 0; i < list.size(); i++) {
				Node tag = list.elementAt(i);
				if (tag instanceof LinkTag)// <a> 标签
				{
					LinkTag link = (LinkTag) tag;
					String linkUrl = link.getLink();// url

					links.add(linkUrl);
				} else// <frame> 标签
				{
					// 提取 frame 里 src 属性的链接如 <frame src="test.html"/>
					String frame = tag.getText();
					int start = frame.indexOf("src=");
					frame = frame.substring(start);
					int end = frame.indexOf(" ");
					if (end == -1)
						end = frame.indexOf(">");
					String frameUrl = frame.substring(5, end - 1);

					links.add(frameUrl);
				}
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
		for (String link : links)
			System.out.println(link);
		return links;
	}

	private static void writerTxt(String string, String title) {
		BufferedWriter fw = null;

		try {

			File file = new File("D:\\tourist_done\\" + title);
			fw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true), "utf-8")); // 指定编码格式，以免读取时中文字符异常

			fw.append(string);
			fw.newLine();
			fw.flush(); // 全部写入缓存中的内容
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void parserTheDiv(Parser parser, NodeFilter nodeFilter)
			throws ParserException {

		NodeList nodelist2 = parser.parse(nodeFilter);// 过滤出符合filter_text的节点LIST
		Node[] nodes = nodelist2.toNodeArray();// 转化为数组
		StringBuffer buftext = new StringBuffer();
		String line = null;
		for (int i = 0; i < nodes.length; i++) {// 循环加到buftext上
			line = nodes[i].toPlainTextString();
			if (line != null) {
				buftext.append(line);
			}
		}
		String body = buftext.toString();
		System.out.println(body);// 输出
	}
}