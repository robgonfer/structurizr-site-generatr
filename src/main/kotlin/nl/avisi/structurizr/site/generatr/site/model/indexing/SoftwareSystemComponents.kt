package nl.avisi.structurizr.site.generatr.site.model.indexing

import com.structurizr.model.Component
import com.structurizr.model.SoftwareSystem
import nl.avisi.structurizr.site.generatr.includedSoftwareSystems
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

    val allSoftwareSystems = softwareSystem.model.softwareSystems        
    val allExternalSoftwareSystems = allSoftwareSystems.filter { x -> x.hasTag("FakeExternalSystem") ||  x.hasTag("External System")}
    
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


        //Add linked components and External SS
        if (dig != null)
        {
            //Find other views with it
            allDigs.forEach { view ->
                view.elements.forEach { el ->
                    if (el.element.name == it.name)
                    {
                        documents += Document(
                                GetUrl(view.key, href),
                                "Component views",
                                "${softwareSystem.name} | Component views | ${view.container.name} | ${it.name}",
                                it.name)
                    }
                }
            }                
            //Need to find all components with a relationship with IT as destination
            val componentsWithRel = mutableListOf<Component>()

            allComponents.forEach { c ->
                c.relationships
                        .filter { r -> r.destination.id.equals(it.id) }
                        .forEach {rl ->
                            componentsWithRel.add(c)
                        }
            }

            componentsWithRel.forEach { linkedct ->

                val relDig = allDigs.firstOrNull { v -> v.container.id == linkedct.container.id }

                if (relDig?.key?.isNotEmpty() == true)
                {

                }

            };

            //Now all external software systems. Get Inbound
            val externalSSWithRel = mutableListOf<SoftwareSystem>()

            allExternalSoftwareSystems.forEach { s ->
                s.relationships
                        .filter { r -> r.destination.id.equals(it.id) }
                        .forEach {rl ->
                            externalSSWithRel.add(s)
                        }
            }

            externalSSWithRel.forEach { linkedExternalSS ->

                val relDig = diagrams.firstOrNull { v -> v.container.id == it.container.id }

                if (relDig?.key?.isNotEmpty() == true)
                {

                    documents += Document(
                            GetUrl(relDig.key, href),
                            "Component views",
                            "${softwareSystem.name} | Component views | ${it.container.name} | ${it.name} | ${linkedExternalSS.name} | (INBOUND)",
                            linkedExternalSS.name)

                    documents += Document(
                            GetUrl(relDig.key, href),
                            "Component views",
                            "${softwareSystem.name} | Component views | ${it.container.name} | ${it.name} | ${linkedExternalSS.name} | (INBOUND)",
                            it.name)                    
                }

            };
                
            //Now outbound calls to external systems
            val externalSSWithOutRel = mutableListOf<SoftwareSystem>()

            allExternalSoftwareSystems.forEach { s ->
                it.relationships
                        .filter { r -> r.destination.id.equals(s.id) }
                        .forEach { se ->
                            externalSSWithOutRel.add(s)
                        }
            }

            externalSSWithOutRel.forEach { linkedExternalSS ->

                val relDig = diagrams.firstOrNull { v -> v.container.id == it.container.id }

                if (relDig?.key?.isNotEmpty() == true)
                {

                    documents += Document(
                            GetUrl(relDig.key, href),
                            "Component views",
                            "${softwareSystem.name} | Component views | ${it.container.name} | ${it.name} |  ${linkedExternalSS.name} | (OUTBOUND)",
                            linkedExternalSS.name)
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
