# IfcRDF-to-IFC_converter
Converter from IfcRDF to IFC.
This is a converter to convert ifcOWL instance files back to IFC models.
* It only supports generated TTL file from IFC-to-RDF-converter on https://github.com/mmlab/IFC-to-RDF-converter.
* Compiled jar file in the JAR folder. Command line goes: java -jar RDF2IFC-0.0.1-SNAPSHOT.jar <input.ttl> <output.ifc>
* Comparing with original IFC file, the round trip IFC file has fewer entities. This is because ifcOWL files have merged all identical entities. 
