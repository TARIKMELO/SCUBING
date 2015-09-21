package bll.data_structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.management.timer.TimerMBean;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import presentation.layout.MapFrame;
import bll.data_structures.nodes.DimensionTypeValue;
import bll.data_structures.nodes.MeasureTypeValue;
import bll.data_structures.nodes.NodeSimple;
import bll.parallel.ResourceII;
import dal.drivers.CubeColumn;
import dal.drivers.IResultSetText;
import dal.drivers.ShapeFileReader;
import dal.drivers.ShapeFileWriter;

public class PerformCube<N extends NodeSimple<DimensionTypeValue>> {

	private FeatureSource<SimpleFeatureType, SimpleFeature> CreateCube(HashMap<String, CubeColumn> cubeColumns, int x, int y, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) throws Exception{
		//conectar
		ShapeFileReader<DimensionTypeValue> shapeFileReader = new ShapeFileReader<DimensionTypeValue>(featureSource,cubeColumns);
		IResultSetText<DimensionTypeValue> rs = shapeFileReader.getData();

		//Cálculo do tempo de computação do cubo
		long tempoInicial = System.currentTimeMillis();  

		ICubeSimple<DimensionTypeValue> cube = createBaseCuboide(rs, cubeColumns);
		//cubeGrid.performHierarchies(x,y,rs, shapeFileReader.getSource(),cubeColumns);
		cube.generateAggregations();
		ResourceII<Entry <ArrayList<DimensionTypeValue>, ArrayList<MeasureTypeValue>>> resource= cube.cubeToTable();




		ShapeFileWriter shapeFileWriter = new ShapeFileWriter(cubeColumns);


		//FeatureSource sourceDesti = shapeFileWriter.insertCubeToSource(hashResult, shapeFileReader.getSource());
		FeatureSource<SimpleFeatureType, SimpleFeature> sourceDesti = shapeFileWriter.insertCubeToSource(resource, shapeFileReader.getSource());


		//Cálculo do tempo de computação do cubo
		long tempoFinal = System.currentTimeMillis();  
		System.out.println("Tempo em milisegundos: "+ (tempoFinal - tempoInicial) );
		System.out.println("Tempo em segundos: "+ (tempoFinal - tempoInicial) / 1000d);

		return sourceDesti;
	}

	public ICubeSimple<DimensionTypeValue> createBaseCuboide(IResultSetText<DimensionTypeValue> rs,HashMap<String, CubeColumn> cubeColumns )
	{

		//TODO: Olhar o padrao adapter para colocar o nome da coluna ao invés do indice
		//-1 por causa da coluna que é a medida
		ICubeSimple<DimensionTypeValue> cube = new CubeSimple<DimensionTypeValue>(cubeColumns,cubeColumns.size());


		Object[] tuple;
		ArrayList<MeasureTypeValue>  measures;
		String measureValue;
		while((tuple=rs.next())!=null){

			measures = new ArrayList<MeasureTypeValue>();
			for (Entry<String, CubeColumn> cubeColumn: cubeColumns.entrySet())
			{
				if(cubeColumn.getValue().isMeasure())
				{
					//measures = new ArrayList<MeasureTypeValue>();
					measureValue = tuple[cubeColumn.getValue().getIndex()].toString().trim();
					measures.add(new MeasureTypeValue(  measureValue,cubeColumn.getValue().getColumnName()));
				}
			}
			for (Entry<String, CubeColumn> cubeColumn: cubeColumns.entrySet())
			{
				if(!cubeColumn.getValue().isMeasure())
				{
					Object attributeO = tuple[cubeColumn.getValue().getIndex()];
					String attribute = attributeO.toString();
					DimensionTypeValue typeValu = new DimensionTypeValue(attribute, cubeColumn.getKey());
					NodeSimple<DimensionTypeValue> n = cube.findNode(typeValu);
					if(n == null){
						n = new NodeSimple<DimensionTypeValue>(cubeColumns, measures);
						cube.insertNode(typeValu, n);
					}
					else
					{
						n.updateMeasure(measures);
					}
				}
			}
			cube.refresh();
		}

		return cube;
	}

	public void gerarCubo(HashMap<String, CubeColumn> cubeColumns, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) 
	{
		HashMap<Integer, ArrayList<CubeColumn> > hierarquias = new HashMap<Integer, ArrayList<CubeColumn> >();
		try
		{
			final HashMap<String, CubeColumn> commonCubeColumns = new HashMap<String, CubeColumn>();
			for (CubeColumn cubeColumn : cubeColumns.values()) {
				if (cubeColumn.isMeasure()||cubeColumn.getHierarchy()==-1)
				{
					commonCubeColumns.put(cubeColumn.getColumnName(), cubeColumn);
				}

			}


			for (CubeColumn cubeColumn : cubeColumns.values()) {
				if (!cubeColumn.isMeasure())
				{
					if (cubeColumn.getHierarchy()!=-1){

						if (hierarquias.get(cubeColumn.getHierarchy())==null)
						{
							hierarquias.put(cubeColumn.getHierarchy(),new ArrayList<CubeColumn>()); 
							hierarquias.get(cubeColumn.getHierarchy()).addAll(commonCubeColumns.values());
						}
						hierarquias.get(cubeColumn.getHierarchy()).add(cubeColumn);
					}
				}
			}

			//Deepy Copy
			HashMap<String, CubeColumn> cubeColumnsAux = new HashMap<String, CubeColumn>();

			for (ArrayList<CubeColumn> cubeColumnList : hierarquias.values()) {
				int auxT = 0;
				for (CubeColumn cubeColumn : cubeColumnList) {
					cubeColumn.setIndex(auxT);
					auxT++;
					cubeColumnsAux.put(cubeColumn.getColumnName(), cubeColumn);
				}
				try {
					FeatureSource<SimpleFeatureType, SimpleFeature> sourceDesti;
					sourceDesti = CreateCube(cubeColumnsAux,0 ,0, featureSource);
					MapFrame.getInstance().createLayer(sourceDesti);
					cubeColumnsAux = new HashMap<String, CubeColumn>();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
		catch(Exception ioEx){
			ioEx.printStackTrace();
		}
	}

}
