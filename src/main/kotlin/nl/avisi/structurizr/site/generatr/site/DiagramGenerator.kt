package nl.avisi.structurizr.site.generatr.site

import com.structurizr.Workspace
import com.structurizr.export.Diagram
import com.structurizr.export.plantuml.PlantUMLDiagram
import com.structurizr.view.ModelView
import com.structurizr.view.View
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

fun generateDiagrams(workspace: Workspace, exportDir: File) {
    val pumlDir = pumlDir(exportDir)
    val svgDir = svgDir(exportDir)
    val pngDir = pngDir(exportDir)

    val plantUMLDiagrams = generatePlantUMLDiagrams(workspace)

    plantUMLDiagrams.parallelStream()
        .forEach { diagram ->
            val plantUMLFile = File(pumlDir, "${diagram.key}.puml")
            if (!plantUMLFile.exists() || plantUMLFile.readText() != diagram.definition) {
                println("${diagram.key}...")
                saveAsSvg(diagram, svgDir)
                saveAsPng(diagram, pngDir)
                saveAsPUML(diagram, plantUMLFile)
            } else {
                println("${diagram.key} UP-TO-DATE")
            }
        }
}

fun generateDiagramWithElementLinks(
    workspace: Workspace,
    view: View,
    url: String,
    diagramCache: ConcurrentHashMap<String, String>
): String {
    val diagram = generatePlantUMLDiagramWithElementLinks(workspace, view, url)

    val name = "${diagram.key}-${view.key}"
    return diagramCache.getOrPut(name) {
        val reader = SourceStringReader(diagram.withCachedIncludes().definition)
        val stream = ByteArrayOutputStream()

        reader.outputImage(stream, FileFormatOption(FileFormat.SVG, false))
        AddHighlightToSvg(stream.toString(Charsets.UTF_8))
    }
}

private fun generatePlantUMLDiagrams(workspace: Workspace): Collection<Diagram> {
    val plantUMLExporter = PlantUmlExporter(workspace)

    return plantUMLExporter.export()
}

 private fun AddHighlightToSvg(content: String)
            : String {

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xml = InputSource(StringReader(content))
        val doc = builder.parse(xml)
        val xPath = XPathFactory.newInstance().newXPath()
        val gNodes = xPath.compile("//*[local-name()='g' and starts-with(@id, 'elem_')]").evaluate(doc, XPathConstants.NODESET) as NodeList

        val len = gNodes.length - 1

        for (i in 0..len) {

            val node = gNodes.item(i)

            val rect = xPath.compile(".//*[local-name()='rect']").evaluate(node, XPathConstants.NODE) as Node
            val rectId = node.attributes.getNamedItem("id").nodeValue.replace("elem_", "")  + ".BoxH"

            val attr1 = doc.createAttribute("onmouseover")
            attr1.value = "addHighlight2(event,'${rectId}')";

            val attr2 = doc.createAttribute("onmouseout")
            attr2.value = "removeHighlight2('${rectId}')";

            val attr3 = doc.createAttribute("id");
            attr3.value = rectId;

            rect.attributes.setNamedItem(attr1)
            rect.attributes.setNamedItem(attr2)
            rect.attributes.setNamedItem(attr3)
        }

        val tooltipChild = doc.createElement("g");
        tooltipChild.setAttribute("id", "tooltip")
        tooltipChild.setAttribute("visibility", "hidden")        
        
        val firstRectNode = doc.createElement("rect");
        firstRectNode.setAttribute("x","2");
        firstRectNode.setAttribute("y","2");
        firstRectNode.setAttribute("width","80");
        firstRectNode.setAttribute("height","24");
        firstRectNode.setAttribute("fill","black");
        firstRectNode.setAttribute("opacity","0.4");
        firstRectNode.setAttribute("rx","2");
        firstRectNode.setAttribute("ry","2");
        tooltipChild.appendChild(firstRectNode);

        val secondRectNode = doc.createElement("rect");
        secondRectNode.setAttribute("width","80");
        secondRectNode.setAttribute("height","240");
        secondRectNode.setAttribute("fill","#009cdc");
        secondRectNode.setAttribute("rx","2");
        secondRectNode.setAttribute("ry","2");
        tooltipChild.appendChild(secondRectNode);

        val thirdTextNode = doc.createElement("text");
        thirdTextNode.setAttribute("x","4");
        thirdTextNode.setAttribute("y","6");
        thirdTextNode.setAttribute("style","font:arial;fill='red';stroke='#0000FF'");
        thirdTextNode.setAttribute("rx","2");
        thirdTextNode.textContent = "Tooltip";        
        tooltipChild.appendChild(thirdTextNode);                
        
        doc.lastChild.appendChild((tooltipChild))

        val js =  "<![CDATA[   function getId(identifier)   {	   var tokens = identifier.split(\".\");	   return tokens[tokens.length - 1];   }   function addHighlight2(evt, id) {      var crmElem = document.getElementById(id);      crmElem.classList.add(\"boxH\");      var prefix = id.replace(\".BoxH\", \"\") + \"-to-\";      var paths = document.querySelectorAll('[id^=\"' + prefix + '\"]');      //NEW CODE      var links = \"Outgoing Links:|\";	  var displayOutgoingInfoToolTip = false;	  var displayIncomingInfoToolTip = false;	        //END NEW CODE      for (i = 0; i < paths.length; ++i) {		 displayOutgoingInfoToolTip = true;         paths[i].classList.add(\"pathH\");         var tokens = paths[i].id.replace(prefix,\"\");          if (i == (paths.length - 1)) {            links += getId(tokens);         } else {            links += getId(tokens) + \"|\";         }      }	  	  var shortenedId = id.replace(\".BoxH\", \"\");	  var shortIdTokens = shortenedId.split(\".\");	  var shortId = shortIdTokens[shortIdTokens.length - 1];	  	  paths = document.querySelectorAll('[id$=\"' + shortId + '\"]');	  	  var hasIncomingLink = false;      for (i = 0; i < paths.length; ++i) {		 if (paths[i].tagName == \"path\")		 {			hasIncomingLink = true;			break;		 }      }	  	  if (hasIncomingLink)	  {	     if (displayOutgoingInfoToolTip)		 {			links += \"| |\" + \"Incoming Links:|\";		 }		 else		 {			links = \"Incoming Links:|\"		 }	  }	        for (i = 0; i < paths.length; ++i) {		 if (paths[i].tagName == \"path\")		 {			displayIncomingInfoToolTip = true;			var idx = paths[i].id.indexOf(\"-to\");			var tText =  paths[i].id.substring(0,idx);			links += getId(tText) + \"|\";		 }      }	  	        //NEW CODE      if (displayOutgoingInfoToolTip || displayIncomingInfoToolTip) {         var svg = document.getElementsByTagName('svg')[0]		         var tooltip = document.getElementById('tooltip');         var tooltipText = tooltip.getElementsByTagName('text')[0];         var tooltipRects = tooltip.getElementsByTagName('rect');         var CTM = svg.getScreenCTM();         var x = (evt.clientX - CTM.e + 6) / CTM.a;         var y = (evt.clientY - CTM.f + 20) / CTM.d;         tooltip.setAttributeNS(null, \"transform\", \"translate(\" + x + \" \" + y + \")\");         tooltip.setAttributeNS(null, \"visibility\", \"visible\");         tooltipText.firstChild.data = links;         var words = tooltipText.firstChild.data.split('|');         var allTspan = tooltipText.querySelectorAll(\"tspan\");         allTspan.forEach((child) => {            tooltipText.removeChild(child);         })         createMultiline(tooltipText, tooltipRects);      }      //END NEW CODE		   }         function removeHighlight2(id) {         //NEW CODE         var tooltip = document.getElementById('tooltip');         tooltip.setAttributeNS(null, \"visibility\", \"hidden\");         //END NEW CODE            var crmElem = document.getElementById(id);         crmElem.classList.remove(\"boxH\");            var prefix = id.replace(\".BoxH\", \"\") + \"-to-\";         var paths = document.querySelectorAll('[id^=\"' + prefix + '\"]');            for (i = 0; i < paths.length; ++i)          {            paths[i].classList.remove(\"pathH\")         }      }]]>"
        val scriptChild = doc.createElement("script")
        scriptChild.appendChild(doc.createTextNode(js))
        

        val styleChild = doc.createElement("style");
        val attrType = doc.createAttribute("type");
        attrType.value = "text/css";
        val css = "<![CDATA[			.boxH {				fill: red;			}			.pathH {				stroke: red !important;				stroke-width:3.0 !important;			}			path:hover { 			stroke: #FF0000 !important;			}			#tooltip {				dominant-baseline: hanging;				background-color:#33FF00;				font: bold 15px sans-serif;				fill: white;						}									]]>"
        styleChild.appendChild(doc.createTextNode(css))
        //doc.lastChild.appendChild((styleChild))

        val tf = TransformerFactory.newInstance()
        val transformer = tf.newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        val output = writer.buffer.toString()
                .replace("\n|\r".toRegex(), "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")

        return output
    }

private fun saveAsPUML(diagram: Diagram, plantUMLFile: File) {
    plantUMLFile.writeText(diagram.definition)
}

private fun saveAsSvg(diagram: Diagram, svgDir: File, name: String = diagram.key) {
    val reader = SourceStringReader(diagram.withCachedIncludes().definition)
    val svgFile = File(svgDir, "$name.svg")

    svgFile.outputStream().use {
        reader.outputImage(it, FileFormatOption(FileFormat.SVG, false))
    }
}

private fun saveAsPng(diagram: Diagram, pngDir: File) {
    val reader = SourceStringReader(diagram.withCachedIncludes().definition)
    val pngFile = File(pngDir, "${diagram.key}.png")

    pngFile.outputStream().use {
        reader.outputImage(it)
    }
}

private fun generatePlantUMLDiagramWithElementLinks(workspace: Workspace, view: View, url: String): Diagram {
    val plantUMLExporter = PlantUmlExporterWithElementLinks(workspace, url)

    return plantUMLExporter.export(view)
}

private fun pumlDir(exportDir: File) = File(exportDir, "puml").apply { mkdirs() }
private fun svgDir(exportDir: File) = File(exportDir, "svg").apply { mkdirs() }
private fun pngDir(exportDir: File) = File(exportDir, "png").apply { mkdirs() }

private fun Diagram.withCachedIncludes(): Diagram {
    val def = definition.replace("!include\\s+(.*)".toRegex()) {
        val cachedInclude = IncludeCache.cachedInclude(it.groupValues[1])
        "!include $cachedInclude"
    }

    return PlantUMLDiagram(view as ModelView, def)
}

private object IncludeCache {
    private val cache = mutableMapOf<String, String>()
    private val cacheDir = File("build/puml-cache").apply { mkdirs() }

    fun cachedInclude(includedFile: String): String {
        if (!includedFile.startsWith("http"))
            return includedFile

        return cache.getOrPut(includedFile) {
            val fileName = includedFile.split("/").last()
            val cachedFile = File(cacheDir, fileName)

            if (!cachedFile.exists())
                downloadIncludedFile(includedFile, cachedFile)

            cachedFile.absolutePath
        }
    }

    private fun downloadIncludedFile(includedFile: String, cachedFile: File) {
        URL(includedFile).openStream().use { inputStream ->
            cachedFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
