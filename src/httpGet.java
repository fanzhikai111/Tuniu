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

	// ���������ַ�
	public static String StringFilter(String str) throws PatternSyntaxException {
		// ֻ������ĸ������
		// String regEx = "[^a-zA-Z0-9]";
		// ��������������ַ�
		String regEx = "[`~!@#$%^&*()+=|{}':;',//[//].<>/?~��@#��%����&*��������+|{}������������������������]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	}
//�������ʱ��Ϊ�棬���ݿ��б��Ϊ1�����ʳ�ʱ���Ϊ�٣����ݿ��б��Ϊ-1
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
			// ���� <frame >��ǩ�� filter��������ȡ frame ��ǩ��� src ��������ʾ������
			NodeFilter frameFilter = new NodeFilter() {
				public boolean accept(Node node) {
					if (node.getText().startsWith("frame src=")) {
						return true;
					} else {
						return false;
					}
				}
			};
			// OrFilter �����ù��� <a> ��ǩ���� <frame> ��ǩ
			OrFilter linkFilter = new OrFilter(new NodeClassFilter(
					LinkTag.class), frameFilter);
			// �õ����о������˵ı�ǩ
			NodeList list = parser.extractAllNodesThatMatch(linkFilter);
			for (int i = 0; i < list.size(); i++) {
				Node tag = list.elementAt(i);
				if (tag instanceof LinkTag)// <a> ��ǩ
				{
					LinkTag link = (LinkTag) tag;
					String linkUrl = link.getLink();// url

					links.add(linkUrl);
				} else// <frame> ��ǩ
				{
					// ��ȡ frame �� src ���Ե������� <frame src="test.html"/>
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
					new FileOutputStream(file, true), "utf-8")); // ָ�������ʽ�������ȡʱ�����ַ��쳣

			fw.append(string);
			fw.newLine();
			fw.flush(); // ȫ��д�뻺���е�����
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

		NodeList nodelist2 = parser.parse(nodeFilter);// ���˳�����filter_text�Ľڵ�LIST
		Node[] nodes = nodelist2.toNodeArray();// ת��Ϊ����
		StringBuffer buftext = new StringBuffer();
		String line = null;
		for (int i = 0; i < nodes.length; i++) {// ѭ���ӵ�buftext��
			line = nodes[i].toPlainTextString();
			if (line != null) {
				buftext.append(line);
			}
		}
		String body = buftext.toString();
		System.out.println(body);// ���
	}
}