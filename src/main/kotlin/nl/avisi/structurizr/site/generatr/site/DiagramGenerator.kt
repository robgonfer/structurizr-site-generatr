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
        tooltipChild.nodeValue = "<rect x=\"2\" y=\"2\" width=\"80\" height=\"24\" fill=\"black\" opacity=\"0.4\" rx=\"2\" ry=\"2\"/>\t\t<rect width=\"80\" height=\"240\" fill=\"#009cdc\" rx=\"2\" ry=\"2\"/>\t\t<text x=\"4\" y=\"6\" style=\"font:arial;fill='red';stroke='#0000FF'\">Tooltip</text>"
        doc.lastChild.appendChild((tooltipChild))

        val js =  "<![CDATA[   function addHighlight(id) {      var crmElem = document.getElementById(id);      crmElem.classList.add("boxH");      var prefix = id.replace(".BoxH", "") + "-to-";      var paths = document.querySelectorAll('[id^="' + prefix + '"]');      for (i = 0; i < paths.length; ++i) {         ;         paths[i].classList.add("pathH")      }   }   function addHighlight2(evt, id) {      var crmElem = document.getElementById(id);      crmElem.classList.add("boxH");      var prefix = id.replace(".BoxH", "") + "-to-";      var paths = document.querySelectorAll('[id^="' + prefix + '"]');      //NEW CODE      var outoingLinks = "Outgoing Links:|";      //END NEW CODE      for (i = 0; i < paths.length; ++i) {         paths[i].classList.add("pathH");         var tokens = paths[i].id.split('.');         if (i == (paths.length - 1)) {            outoingLinks += tokens[tokens.length - 1];         } else {            outoingLinks += tokens[tokens.length - 1] + "|";         }      }      //NEW CODE      if (outoingLinks != "Outgoing Links:|") {         var svg = document.getElementsByTagName('svg')[0]         //var svg = document.getElementById('tooltip-svg-6');		         var tooltip = document.getElementById('tooltip');         var tooltipText = tooltip.getElementsByTagName('text')[0];         var tooltipRects = tooltip.getElementsByTagName('rect');         var CTM = svg.getScreenCTM();         var x = (evt.clientX - CTM.e + 6) / CTM.a;         var y = (evt.clientY - CTM.f + 20) / CTM.d;         tooltip.setAttributeNS(null, "transform", "translate(" + x + " " + y + ")");         tooltip.setAttributeNS(null, "visibility", "visible");         tooltipText.firstChild.data = outoingLinks;         var words = tooltipText.firstChild.data.split('|');         var allTspan = tooltipText.querySelectorAll("tspan");         allTspan.forEach((child) => {            tooltipText.removeChild(child);         })         createMultiline(tooltipText, tooltipRects);      }      //END NEW CODE		   }   function removeHighlight(id) {      var crmElem = document.getElementById(id);      crmElem.classList.remove("boxH");      var prefix = id.replace(".BoxH", "") + "-to-";      var paths = document.querySelectorAll('[id^="' + prefix + '"]');      for (i = 0; i < paths.length; ++i) {         paths[i].classList.remove("pathH")      }   }   function removeHighlight2(id) {      //NEW CODE      var tooltip = document.getElementById('tooltip');      tooltip.setAttributeNS(null, "visibility", "hidden");      //END NEW CODE      var crmElem = document.getElementById(id);      crmElem.classList.remove("boxH");      var prefix = id.replace(".BoxH", "") + "-to-";      var paths = document.querySelectorAll('[id^="' + prefix + '"]');      for (i = 0; i < paths.length; ++i)       {         paths[i].classList.remove("pathH")      }   }   function createMultiline(textElement, tooltipRects) {      var words = textElement.firstChild.data.split('|');      var startX = textElement.getAttributeNS(null, 'x');      var width = parseFloat(textElement.getAttributeNS(null, 'data-width'));      var width = parseFloat(10);      var maxClientLength = 0;      // Clear original text      textElement.firstChild.data = "";      // Create first tspan element      var tspanElement = document.createElementNS("http://www.w3.org/2000/svg", "tspan");      // Create text in tspan element      var textNode = document.createTextNode(words[0]);      // Add tspan element to DOM      tspanElement.appendChild(textNode);      // Add text to tspan element      textElement.appendChild(tspanElement);      if (tspanElement.getComputedTextLength() > maxClientLength) {         maxClientLength = tspanElement.getComputedTextLength();      }      for (var i = 1; i < words.length; i++) {         var len = textNode.data.length;         // Add next word         tspanElement.firstChild.data += " " + words[i];         if (tspanElement.getComputedTextLength() > width) {            // Remove added word            tspanElement.firstChild.data = tspanElement.firstChild.data.slice(0, len);            // Create new tspan element            tspanElement = document.createElementNS("http://www.w3.org/2000/svg", "tspan");            tspanElement.setAttributeNS(null, "x", startX);            tspanElement.setAttributeNS(null, "dy", 20);            textNode = document.createTextNode(words[i]);            tspanElement.appendChild(textNode);            textElement.appendChild(tspanElement);            if (tspanElement.getComputedTextLength() > maxClientLength) {               maxClientLength = tspanElement.getComputedTextLength();            }         }      }      for (var i = 0; i < tooltipRects.length; i++) {         tooltipRects[i].setAttributeNS(null, "width", maxClientLength + 20);         tooltipRects[i].setAttributeNS(null, "height", (words.length * 20) + 20);      }   }]]>"
        val scriptChild = doc.createElement("script")
        scriptChild.appendChild(doc.createTextNode(js))
        doc.lastChild.appendChild(scriptChild)

        val styleChild = doc.createElement("style");
        val attrType = doc.createAttribute("type");
        attrType.value = "text/css";
        val css = "<![CDATA[			.boxH {				fill: red;			}			.pathH {				stroke: red !important;				stroke-width:3.0 !important;			}			path:hover { 			stroke: #FF0000 !important;			}			#tooltip {				dominant-baseline: hanging;				background-color:#33FF00;				font: bold 15px sans-serif;				fill: white;						}									]]>"
        styleChild.appendChild(doc.createTextNode(css))
        doc.lastChild.appendChild((styleChild))

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
