package nl.avisi.structurizr.site.generatr.site.model.indexing

import com.structurizr.model.Component
import com.structurizr.model.SoftwareSystem
import nl.avisi.structurizr.site.generatr.site.GeneratorContext
import nl.avisi.structurizr.site.generatr.site.asUrlToDirectory
import nl.avisi.structurizr.site.generatr.site.model.PageViewModel
import nl.avisi.structurizr.site.generatr.site.model.SoftwareSystemPageViewModel

fun softwareSystemComponents(softwareSystem: SoftwareSystem, viewModel: PageViewModel) = softwareSystem.containers
    .sortedBy { it.name }
    .flatMap { container ->
        container.components
            .sortedBy { it.name }
            .flatMap { component ->
                listOf(component.name, component.description, component.technology)
            }
    }
    .filter { it != null && it.isNotBlank() }
    .joinToString(" ")
    .ifBlank { null }
    ?.let {
        Document(
            SoftwareSystemPageViewModel.url(softwareSystem, SoftwareSystemPageViewModel.Tab.COMPONENT)
                .asUrlToDirectory(viewModel.url),
            "Component views",
            "${softwareSystem.name} | Component views",
            it
        )
    }

    fun softwareSystemComponentsComponent(softwareSystem: SoftwareSystem, viewModel: PageViewModel, generatorContext: GeneratorContext) : List<Document> {

    var components = softwareSystem.containers
            .sortedBy { it.name }
            .flatMap { container -> container.components }

    var documents = emptyList<Document>().toMutableList()

    val diagrams = generatorContext.workspace.views.componentViews
            .filter { it.softwareSystem == softwareSystem}
            .sortedBy { it.key }    

    components.forEach {

        val dig = diagrams.firstOrNull { v -> v.container.id == it.container.id }
        
        val href = SoftwareSystemPageViewModel.url(softwareSystem, SoftwareSystemPageViewModel.Tab.COMPONENT)
                .asUrlToDirectory(viewModel.url)        
        documents += Document(
                GetUrl(dig?.key ?: "NOTFOUND", href),
                "Component views",
                "${softwareSystem.name} | Component views | ${it.container.name} | ${it.name}",
                it.name)

        //Add linked components
        if (dig != null)
        {
            val allDigs = generatorContext.workspace.views.componentViews
                    .sortedBy { it.key }

            it.relationships.forEach { linkedct ->

                //This is actually the destination
                val source = linkedct.source as? Component

                if (source != null) {

                    val relDig = allDigs.firstOrNull { v -> v.container.id == source.container.id }

                    if (relDig?.key?.isNotEmpty() == true)
                    {

                        documents += Document(
                                GetUrl(relDig.key, href),
                                "Component views",
                                "${softwareSystem.name} | Component views | ${source.container.name} | ${source.name} | (INBOUND)",
                                it.name)
                    }
                }
            };
        }        
    }

    return documents
}

private fun GetUrl(componentId : String, defaultUrl : String) : String
{
    return "https://c4.lebara.com/master/svg/$componentId.svg"
}    
