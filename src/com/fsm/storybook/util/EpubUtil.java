package com.fsm.storybook.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;

import android.content.Context;
import android.util.Log;

import com.fsm.storybook.model.BookData;
import com.fsm.storybook.model.Epub3Exception;
import com.fsm.storybook.model.Epub3ManifestItem;
import com.fsm.storybook.model.Epub3Package;
import com.fsm.storybook.model.Epub3TocItem;

public class EpubUtil {
	
	private static EpubUtil mInstance = null;
	private static Context context = null;
	private BookDatabase bkdb;
	
	public static EpubUtil getInstance(Context ctx) {
		context = ctx.getApplicationContext();
		if (mInstance == null) {
	    	mInstance = new EpubUtil(ctx.getApplicationContext());
	    }
	    return mInstance;
	}
	private EpubUtil(Context ctx) {
		context = ctx;
		bkdb = new BookDatabase(context);
	}		

	public void unzipEpubToCache(int bookCode) throws IOException
	{
		BookData bd = bkdb.fetchBook(bookCode);
		if (bd.getEpubFileName()==null || bd.getEpubFileName().isEmpty()) throw new IOException("No epub file");
		File epubFile = new File(bkdb.getBookPath(), bd.getEpubFileName());
		ZipFile zipFile = new ZipFile(epubFile);
		
		File bookDir = new File(context.getCacheDir(), "fsmbook_epub_"+String.valueOf(bookCode));
		if (!bookDir.exists()) bookDir.mkdir();
		
	    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	    while (entries.hasMoreElements()) {
	        ZipEntry entry = entries.nextElement();
	        File entryDestination = new File(bookDir,  entry.getName());
	        entryDestination.getParentFile().mkdirs();
	        if (entry.isDirectory())
	            entryDestination.mkdirs();
	        else {
	            InputStream in = zipFile.getInputStream(entry);
	            OutputStream out = new FileOutputStream(entryDestination);
	            IOUtils.copy(in, out);
	            IOUtils.closeQuietly(in);
	            IOUtils.closeQuietly(out);
	        }
	    }
	}
	
	public void removeEpubFromCache(int bookCode)
	{
		File bookDir = new File(context.getCacheDir(), "fsmbook_epub_"+String.valueOf(bookCode));
		if (bookDir.exists()) bookDir.delete();
	}
	
	public String getCachePath(int bookCode)
	{
		String result = context.getCacheDir().getAbsolutePath()+"/fsmbook_epub_"+String.valueOf(bookCode);
		return result;
	}
	public String getCacheContentPath(int bookCode)
	{
		String result = context.getCacheDir().getAbsolutePath()+"/fsmbook_epub_"+String.valueOf(bookCode)+"/OEBPS";
		return result;
	}

	public Epub3Package parseEpub(int bookCode) throws IOException, Epub3Exception
	{
		String bookPath = getCachePath(bookCode);
		Epub3Package pkg = new Epub3Package(bookPath);
	
		SAXBuilder builder = new SAXBuilder();
	 
		//parse container.xml
		String rootfile = "";
		File containerXML = new File(bookPath+"/META-INF/container.xml");
		try {
			 
			Document document = (Document) builder.build(containerXML);
			Element rootNode = document.getRootElement();
			Element fileNode = rootNode.getChild("rootfiles", rootNode.getNamespace());
			List<Element> rootFiles = fileNode.getChildren("rootfile", rootNode.getNamespace());
			for (int i = 0; i < rootFiles.size(); i++) {
			   Element node = (Element)rootFiles.get(i);
			   if (node.getAttributeValue("media-type").equals("application/oebps-package+xml"))
			   {
				   rootfile = node.getAttributeValue("full-path");
				   break;
			   }
			}
	 
		} catch (IOException io) {
			throw io;
		} catch (JDOMException jdomex) {
			throw new Epub3Exception("Parse container.xml failed, "+jdomex.getMessage());
		} catch(Exception e) {
			throw new Epub3Exception("Parse container.xml failed, "+e.getMessage());
		}
		
		if (rootfile.isEmpty()) throw new Epub3Exception("Can't find rootfile");
		
		//get content path according to full-path
		StringBuffer sb = new StringBuffer();
		String[] pathList = rootfile.split("/");
		for(int i=0 ; i<pathList.length-1 ; i++) {
			if (sb.length()>0) sb.append("/");
			sb.append(pathList[i]);
		}
		pkg.setContentPath(bookPath+"/"+sb.toString());

		//parse rootfile (usually ***.opf)
		File rootfileXML = new File(bookPath+"/"+rootfile);
		try {
			 
			Document document = (Document) builder.build(rootfileXML);
			Element rootNode = document.getRootElement();
			//get metadata
			//List list = rootNode.getChild("metadata", rootNode.getNamespace()).getChildren();
			//for (int i = 0; i < list.size(); i++) {
			//   Element node = (Element) list.get(i);
			//   
			//}
			
			//get manifest items
			ArrayList<Epub3ManifestItem> manifestItems = new ArrayList<Epub3ManifestItem>();
			List<Element> manifestList = rootNode.getChild("manifest", rootNode.getNamespace()).getChildren();
			for (int i = 0; i < manifestList.size(); i++) {
			   Element node = (Element) manifestList.get(i);
			   if (node.getName().equals("item")) {
				   Epub3ManifestItem item = new Epub3ManifestItem(node.getAttributeValue("id"),
						   node.getAttributeValue("properties"),
						   node.getAttributeValue("href"),
						   node.getAttributeValue("media-type"));
				   manifestItems.add(item);
			   }
			}
			pkg.setManifestItems(manifestItems);
			
			//get page progression direction
			Element spineNode = rootNode.getChild("spine", rootNode.getNamespace());
			if (spineNode.getAttributeValue("page-progression-direction")!=null)
				pkg.setPageProgressionDirection(spineNode.getAttributeValue("page-progression-direction"));
			else
				pkg.setPageProgressionDirection("ltr");
			
			//get spine items
			List<Element> spineList = spineNode.getChildren();
			ArrayList<Epub3ManifestItem> spineItems = new ArrayList<Epub3ManifestItem>();
			for (Element spineItem : spineList) {
				String idref = spineItem.getAttributeValue("idref");
				for (Epub3ManifestItem mItem : manifestItems) {
					if (idref.equals(mItem.getId())) {
						spineItems.add(mItem);
						break;
					}
				}
			}
			pkg.setSpineItems(spineItems);
			
		} catch (IOException io) {
			throw io;
		} catch (JDOMException jdomex) {
			throw new Epub3Exception("Parse root file failed, "+jdomex.getMessage());
		} catch(Exception e) {
			throw new Epub3Exception("Parse root file failed, "+e.getMessage());
		}
		
		//parse navigation file, get TOC
		String navPage = pkg.getNavPage();
		if (navPage!=null) {
			File navXML = new File(pkg.getContentPath()+"/"+navPage);
			try {
				Document document = (Document) builder.build(navXML);
				Element rootNode = document.getRootElement();
				ArrayList<Epub3TocItem> tocList = new ArrayList<Epub3TocItem>();
				ElementFilter filter = new ElementFilter("nav", rootNode.getNamespace());
				for(Element navNode:rootNode.getDescendants(filter))
				{
					List<Attribute> attlist = navNode.getAttributes();
					for(Attribute att : attlist) {
						if (att.getName().equals("type")) {
							ElementFilter hrefFilter = new ElementFilter("a", rootNode.getNamespace());
							for(Element nodeHref : navNode.getDescendants(hrefFilter)) {
								tocList.add(new Epub3TocItem(nodeHref.getValue(), nodeHref.getAttributeValue("href")));
							}
							break;
						}
					}
				}		
				pkg.setTocItems(tocList);
			} catch (IOException io) {
				throw io;
			} catch (JDOMException jdomex) {
				throw new Epub3Exception("Parse toc failed, "+jdomex.getMessage());
			} catch (Exception e) {
				throw new Epub3Exception("Parse toc failed, "+e.getMessage());
			}
		}
		
		return pkg;
	}

}

