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
            it.relationships.forEach { linkedct ->

                val destination = linkedct.destination as? Component

                if (destination != null) {

                    val relDig = diagrams.firstOrNull { v -> v.container.id == destination.container.id }

                    if (relDig?.key?.isNotEmpty() == true)
                    {

                        documents += Document(
                                GetUrl(relDig.key, href),
                                "Component views",
                                "${softwareSystem.name} | Component views | ${destination.container.name} | ${destination.name} | (INBOUND)",
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
