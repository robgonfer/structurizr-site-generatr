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

fun externalSystems(generatorContext: GeneratorContext) : List<Document> {

    val allSoftwareSystems = generatorContext.workspace.model.softwareSystems
    val allExternalSoftwareSystems = allSoftwareSystems
            .filter { x -> x.hasTag("FakeExternalSystem") ||  x.hasTag("External System")}
            .flatMap { ss -> listOf(ss.name) }
            .toSet()

    val allComponents = mutableListOf<Component>()
    allSoftwareSystems.forEach {ss ->

        allComponents.addAll(ss.containers
                .flatMap { container -> container.components }
                .toList())

    }

    val documents = emptyList<Document>().toMutableList()

    val allDigs = generatorContext.workspace.views.componentViews
            .sortedBy { it.key }


    allExternalSoftwareSystems.forEach { s ->
        allDigs.forEach { view ->
            view.elements.forEach { el ->
                if (el.element.name == s)
                {
                    documents += Document(
                            GetUrl(view.key),
                            "Component views",
                            "${view.softwareSystem.name} | Component views (External Software Systems) | ${view.container.name} | ${el.element.name}",
                            el.element.name)
                }
            }
        }
    }


    return documents
}

fun softwareSystemComponentsComponent(softwareSystem: SoftwareSystem, viewModel: PageViewModel, generatorContext: GeneratorContext) : List<Document> {

    val allSoftwareSystems = softwareSystem.model.softwareSystems

    val allComponents = mutableListOf<Component>()
    allSoftwareSystems.forEach {ss ->

        allComponents.addAll(ss.containers
                .flatMap { container -> container.components }
                .toList())

    }

    var components = softwareSystem.containers
            .sortedBy { it.name }
            .flatMap { container -> container.components }

    var documents = emptyList<Document>().toMutableList()

    val diagrams = generatorContext.workspace.views.componentViews
            .filter { it.softwareSystem == softwareSystem}
            .sortedBy { it.key }

    val allDigs = generatorContext.workspace.views.componentViews
            .sortedBy { it.key }


    components.forEach {

        val dig = diagrams.firstOrNull { v -> v.container.id == it.container.id }
        val href = SoftwareSystemPageViewModel.url(softwareSystem, SoftwareSystemPageViewModel.Tab.COMPONENT)
                .asUrlToDirectory(viewModel.url)

        if (dig != null)
        {
            //Find all views with this component
            allDigs.forEach { view ->
                view.elements.forEach { el ->
                    if (el.element.name == it.name)
                    {
                        var tags = ""

                        if (it.tags == "Element,Component") tags = "Container"
                        else tags =  it.tags.replace("Element,Component,", "")
                        
                        if (view.containerId == it.container.id) {
                            documents += Document(
                                    GetUrl(view.key),
                                    "Component views",
                                    "${view.softwareSystem.name} | Component Views (Main) | ${view.container.name} | ${it.name}",
                                    tags)
                            
                            documents += Document(
                                    GetUrl(view.key),
                                    "Component views",
                                    "${view.softwareSystem.name} | Component Views (Main) | ${view.container.name} | ${it.name}",
                                    it.tags.replace("Element,Component,", ""))                            
                        }
                        else {
                            documents += Document(
                                    GetUrl(view.key),
                                    "Component views",
                                    "${view.softwareSystem.name} | Component views (Referenced) | ${view.container.name} | ${it.name}",
                                    it.name)


                            documents += Document(
                                    GetUrl(view.key),
                                    "Component views",
                                    "${view.softwareSystem.name} | Component views (Referenced) | ${view.container.name} | ${it.name}",
                                    tags)                            
                        }
                    }
                }
            }

        }
    }

    return documents
}

private fun GetUrl(componentId: String) : String
{
    return "https://c4.lebara.com/master/svg/$componentId.svg"
}
