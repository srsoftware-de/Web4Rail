package de.srsoftware.web4rail;

import java.util.Vector;

import de.srsoftware.tools.Tag;

/**
 * 
 * helper class to create html pages
 * @author Stephan Richter, SRSoftware
 *
 */
public class Page {
	private StringBuffer buf;
	private Vector<String> cssFiles = new Vector<String>();
	private Vector<String> jsFiles = new Vector<String>();

	public Page() {
		buf = new StringBuffer();
	}
	
	public Page append(Object code) {
		buf.append(code);
		return this;
	}
	
	private StringBuffer body(StringBuffer content) {
		return new StringBuffer()
				.append("\t<body>\n")
				.append(content)
				.append("\t</body>\n")
				.append("</html>\n");
	}
	
	private StringBuffer head() {
		StringBuffer sb = new StringBuffer()
				.append("<html>\n")
				.append("\t<head>\n");
		sb.append("\t\t"+new Tag("meta").attr("charset", "UTF-8")+"\n");
		for (String cssFile : cssFiles) {
			sb.append("\t\t"+new Tag("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", cssFile)+"\n");
		}
		for (String jsFile : jsFiles) {
			sb.append("\t\t"+new Tag("script").attr("type", "text/javascript").attr("src", jsFile).content("")+"\n");
		}
		return sb.append("\t</head>\n");
	}
	
	public StringBuffer html() {
		return head().append(body(buf));
	}
	
	public Page js(String jsPath) {
		jsFiles.add(jsPath);
		return this;
	}
	
	public Page style(String cssPath) {
		cssFiles.add(cssPath);
		return this;
	}

	@Override
	public String toString() {
		return head().append(body(buf)).toString();
	}
}
