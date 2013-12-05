/*
 * Code that applies machine learning concepts to improve
 * bug deduplication accuracy in bug repositories.
 * Copyright (C) 2013  Anahita Alipour, Abram Hindle,
 * Tanner Rutgers, Riley Dawson, Finbarr Timbers, Karan Aggarwal
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.io.*;
import java.util.*;

import au.com.bytecode.opencsv.CSVReader;


public class MachineLearnerTableGenerator extends TableGenerator {
	
	private Map<String,Double> priorityMap = new HashMap<String,Double>(){{
		put("Small",1.0);
		put("Medium",2.0);
		put("High",3.0);
		put("Critical",4.0);
		put("Blocker",5.0);
	}};
	
	public double getPriorityNumber(String priority){

		if(priority.equals(""))
			return 0;

		return priorityMap.get(priority);
	}


	public MachineLearnerTableGenerator(boolean doStem, Map<String,Double> freeVars, Corpus inputCorpus) {
		super(doStem,freeVars,inputCorpus);
		// TODO Auto-generated constructor stub
	}

	public void createBMFandCategoryTable(String outputFileName, double dupRatio, boolean includeForbidden){

		Vector<Integer> forbidden = new Vector<Integer>(Arrays.asList(36886,36469,36468,36185,33997,33988,25384,25158,24818,24655,24534,24373,24076,23694,23591,22984,22589,22495,22477,22418,22325,22239,22154,21757,21754,21753,21752,21741,21740,21733,21722,21682,21602,21574,21485,21454,21453,21285,21207,21198,21175,21128,21127,21068,21067,20852,20839,20750,20664,20644,20593,20565,20554,20477,20466,20445,20384,20301,20290,20262,20136,20135,20123,19957,19953,19945,19944,19840,19830,19654,19653,19624,19623,19619,19583,19456,19445,19416,19415,19134,19082,19006,18984,18808,18774,18768,18759,18754,18735,18729,18708,18675,18657,18590,18588,18584,18524,18466,18398,18368,18339,18328,18307,18254,18190,18175,18109,18108,18044,18021,17770,17542,17464,17171,17079,17069,17062,16842,16488,16468,16393,16282,16236,16204,16000,14919,14856,14654,14414,13411,13395,13132,13067,13056,12596,12362,12160,12006,11921,11661,11658,11517,11495,11398,11358,11129,11055,10990,10801,10785,10768,10692,10309,10308,9977,9868,9861,9467,9425,9277,9276,9224,9132,9050,8871,8722,8474,8381,8335,8073,8070,8062,7504,7290,6250,6114,5997,5969,5968,5967,5934,5928,5922,5916,5888,5640,5619,5599));

		try{
			// Create file 
			FileWriter fstream = new FileWriter(outputFileName);
			BufferedWriter out = new BufferedWriter(fstream);
            out.append("Bug 1,Bug 2,bmfu,bmfb,prod,comp,type,prior,vers,class\n");
            Document doc1 = new Document("", "");
			Document doc2 = new Document("", "");
			StringBuffer line = new StringBuffer("");
			String separator = ",";
			double[] features = {};
            int dupCount = 0;

			for(int i=0;i<corpus.getDocuments().size()-1;i++){

				for(int j=i+1;j<corpus.getDocuments().size();j++){

					line = new StringBuffer();
					doc1 = corpus.getDocuments().get(i);
					doc2 = corpus.getDocuments().get(j);
					
					if(!includeForbidden && (forbidden.contains(doc1.getBugID()) || forbidden.contains(doc2.getBugID())))
						continue;
					
					if(!isDuplicate(doc1,doc2))
						continue;

					features = rep.getFeatures(doc1, doc2, freeVariables);
					
					line.append(doc1.getBugID()+separator);
					line.append(doc2.getBugID()+separator);

					for(int index=0;index<features.length;index++){
						line.append( ((double)(Math.round(features[index] * 10000)) / 10000) + separator);
					}
					line.append("dup"+"\n");
					out.append(line);
                    dupCount++;
				}
			}

            Random rand = new Random();
            int totalCount = dupCount;

            while (totalCount < dupCount/dupRatio) {

                line = new StringBuffer();
                int index = rand.nextInt(corpus.getDocuments().size()-1);
                doc1 = corpus.getDocuments().get(index);
                doc2 = corpus.getDocuments().get(index+1);

                if(!includeForbidden && (forbidden.contains(doc1.getBugID()) || forbidden.contains(doc2.getBugID())))
                    continue;

                if(isDuplicate(doc1,doc2))
                    continue;

                features = rep.getFeatures(doc1, doc2, freeVariables);

                line.append(doc1.getBugID()+separator);
                line.append(doc2.getBugID()+separator);

                for(int i=0;i<features.length;i++){
                    line.append( ((double)(Math.round(features[i] * 10000)) / 10000) + separator);
                }
                line.append("non"+"\n");
					out.append(line);
                totalCount++;
            }

			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}


    public void createNewContextualPairTable(String contextFolderName, String outputFileName, double dupRatio, boolean includeForbidden){

        Map<Integer,Double[]> bugContextFeaturesMap = new HashMap<Integer,Double[]>();
        bugContextFeaturesMap = initBugContextFeatures(contextFolderName, corpus);
        Vector<Integer> forbidden = new Vector<Integer>(Arrays.asList(36886,36469,36468,36185,33997,33988,25384,25158,24818,24655,24534,24373,24076,23694,23591,22984,22589,22495,22477,22418,22325,22239,22154,21757,21754,21753,21752,21741,21740,21733,21722,21682,21602,21574,21485,21454,21453,21285,21207,21198,21175,21128,21127,21068,21067,20852,20839,20750,20664,20644,20593,20565,20554,20477,20466,20445,20384,20301,20290,20262,20136,20135,20123,19957,19953,19945,19944,19840,19830,19654,19653,19624,19623,19619,19583,19456,19445,19416,19415,19134,19082,19006,18984,18808,18774,18768,18759,18754,18735,18729,18708,18675,18657,18590,18588,18584,18524,18466,18398,18368,18339,18328,18307,18254,18190,18175,18109,18108,18044,18021,17770,17542,17464,17171,17079,17069,17062,16842,16488,16468,16393,16282,16236,16204,16000,14919,14856,14654,14414,13411,13395,13132,13067,13056,12596,12362,12160,12006,11921,11661,11658,11517,11495,11398,11358,11129,11055,10990,10801,10785,10768,10692,10309,10308,9977,9868,9861,9467,9425,9277,9276,9224,9132,9050,8871,8722,8474,8381,8335,8073,8070,8062,7504,7290,6250,6114,5997,5969,5968,5967,5934,5928,5922,5916,5888,5640,5619,5599));

        try{
            // Create file
            FileWriter fstream = new FileWriter(outputFileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.append("Bug 1, Bug2,");
            for (int i = 1; i<=2; i++) {
                for (char letter = 'a'; letter <= 'z'; letter++) {
                    out.append(letter + " " + i + ",");
                }
            }
            out.append("cosine_similarity,class\n");
            Document doc1 = new Document("", "");
            Document doc2 = new Document("", "");
            StringBuffer line = new StringBuffer("");
            String separator = ",";
            int dupCount = 0;

            for(int i=0;i<corpus.getDocuments().size()-1;i++){

                for(int j=i+1;j<corpus.getDocuments().size();j++){

                    line = new StringBuffer();
                    doc1 = corpus.getDocuments().get(i);
                    doc2 = corpus.getDocuments().get(j);

                    if(!includeForbidden && (forbidden.contains(doc1.getBugID()) || forbidden.contains(doc2.getBugID())))
                        continue;

                    if(!isDuplicate(doc1,doc2))
                        continue;

                    Double[] features1 = bugContextFeaturesMap.get(doc1.getBugID());
                    Double[] features2 = bugContextFeaturesMap.get(doc2.getBugID());

                    line.append(doc1.getBugID()+separator);
                    line.append(doc2.getBugID()+separator);

                    for(int index=0;index<features1.length;index++){
                        line.append( ((double)(Math.round(features1[index] * 10000)) / 10000) + separator);
                    }
                    for(int index=0;index<features2.length;index++){
                        line.append( ((double)(Math.round(features2[index] * 10000)) / 10000) + separator);
                    }

                    line.append(cosineSimilarity(features1, features2) + ",");
                    line.append("dup"+"\n");
                    out.append(line);
                    dupCount++;
                }
            }

            Random rand = new Random();
            int totalCount = dupCount;

            while (totalCount < dupCount/dupRatio) {

                line = new StringBuffer();
                int random = rand.nextInt(corpus.getDocuments().size()-1);
                doc1 = corpus.getDocuments().get(random);
                doc2 = corpus.getDocuments().get(random+1);

                if(!includeForbidden && (forbidden.contains(doc1.getBugID()) || forbidden.contains(doc2.getBugID())))
                    continue;

                if(isDuplicate(doc1,doc2))
                    continue;

                Double[] features1 = bugContextFeaturesMap.get(doc1.getBugID());
                Double[] features2 = bugContextFeaturesMap.get(doc2.getBugID());

                line.append(doc1.getBugID()+separator);
                line.append(doc2.getBugID()+separator);

                for(int index=0;index<features1.length;index++){
                    line.append( ((double)(Math.round(features1[index] * 10000)) / 10000) + separator);
                }
                for(int index=0;index<features2.length;index++){
                    line.append( ((double)(Math.round(features2[index] * 10000)) / 10000) + separator);
                }

                line.append(cosineSimilarity(features1, features2) + ",");
                line.append("non"+"\n");
                out.append(line);
                totalCount++;
            }

            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void createContextualPairTableFromAllFeatures(String inputAllFeaturesFileName, String outputfilename){

        try {
            //csv file containing data
            String strFile = inputAllFeaturesFileName;
            CSVReader reader = new CSVReader(new FileReader(strFile));
            String [] nextLine;
            int lineNumber = 0;

            FileWriter fstream = new FileWriter(outputfilename);
            BufferedWriter out = new BufferedWriter(fstream);

            nextLine = reader.readNext();
            String title = "Bug 1,Bug 2,";
            for (int i = 9; i<nextLine.length-1; i++) {
                title += nextLine[i] + ",";
            }
            title += nextLine[nextLine.length-1] + "\n";
            out.append(title);

            int contextLineConter = 0;
            while ((nextLine = reader.readNext()) != null) {
                contextLineConter++;
                StringBuffer line = new StringBuffer();

                line.append(nextLine[0] + ",").append(nextLine[1] + ",");
                for(int i=9;i<nextLine.length-1;i++){
                    line.append(nextLine[i] + ",");
                }
                line.append(nextLine[nextLine.length-1] + "\n");
                out.append(line);
            }
            out.close();
        }
        catch (Exception e) {
            System.out.println("kangoro");
            // TODO: handle exception
        }
    }

	public void createPrioritiesTable(String outputFileName){

		try{
			// Create file 
			FileWriter fstream = new FileWriter(outputFileName);
			BufferedWriter out = new BufferedWriter(fstream);
			Document doc1 = new Document("", "");
			Document doc2 = new Document("", "");
			StringBuffer line = new StringBuffer("");
			String separator = ",";
			double[] features = {};
			double priority = 0;

			for(int i=0;i<corpus.getDocuments().size();i++){

				for(int j=i+1;j<corpus.getDocuments().size();j++){

					line = new StringBuffer();
					doc1 = corpus.getDocuments().get(i);
					doc2 = corpus.getDocuments().get(j);

					line.append(doc1.getBugID()+separator);
					line.append(doc2.getBugID()+separator);

					priority = reciprocal_function(getPriorityNumber(doc1.getPriority()), getPriorityNumber(doc2.getPriority()));
							
					line.append( ((double)(Math.round(priority * 10000)) / 10000));
					
					out.append(line+"\n");
				}
			}
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void createContextTable(String contextFolderName, String outputFileName, String featureLabels){

		Map<Integer,Double[]> bugContextFeaturesMap = new HashMap<Integer,Double[]>();
		bugContextFeaturesMap = initBugContextFeatures(contextFolderName, corpus);

		try{
			// Create file 
			FileWriter fstream = new FileWriter(outputFileName);
			BufferedWriter out = new BufferedWriter(fstream);
            out.append("bug ID," + featureLabels + "\n");
			for(Document doc: corpus.getDocuments()){

				Double[] features = bugContextFeaturesMap.get(doc.getBugID());
				//	System.out.println("features size: "+features.length);
				StringBuffer line = new StringBuffer("");
				String separator = ",";


				line.append(doc.getBugID()+separator);

				for(int i=0;i<features.length;i++){
					line.append( ((double)(Math.round(features[i] * 10000)) / 10000) + (i==(features.length-1)?"":separator));
				}
				//line.append(doc.getMasterID()!=0?",dup":",non");
				out.append(line+"\n");
			}
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void createJoinTable(String inputBmfFileName, String inputContextFileName, String outputfilename){

		Map<Integer,Double[]> contextMap = new HashMap<Integer,Double[]>();
		String[] contextTitle = {};
		String[] bmf_title = {};

		try {
			//csv file containing data
			String strFile = inputContextFileName;
			CSVReader reader = new CSVReader(new FileReader(strFile));
			String [] nextLine;
			int lineNumber = 0;

			contextTitle = reader.readNext();
			int contextLineConter = 0;
			while ((nextLine = reader.readNext()) != null) {

				contextLineConter++;

				Double[] temp = new Double[nextLine.length-1];
				for(int i=1;i<nextLine.length;i++){
					temp[i-1] = Double.parseDouble(nextLine[i]);
					temp[i-1] = Math.round(temp[i-1]*100)/100.0d;
				}

				contextMap.put(Integer.parseInt(nextLine[0]), temp);
			}
			System.out.println("context lines: "+contextLineConter);
		}
		catch (Exception e) {
			System.out.println("kangoro");
			// TODO: handle exception
		}

		StringBuffer line = new StringBuffer();

		try {

			//csv file containing data
			String strFile = inputBmfFileName;
			CSVReader reader = new CSVReader(new FileReader(strFile));
			String [] nextLine;

			bmf_title = reader.readNext();

			/////************
			FileWriter fstream = new FileWriter(outputfilename);
			BufferedWriter out = new BufferedWriter(fstream);

			for(int i=0;i<bmf_title.length-1;i++)
				out.append(bmf_title[i]+",");

			for(int i=1;i<contextTitle.length;i++)
				out.append(contextTitle[i]+"1,");

			for(int i=1;i<contextTitle.length;i++)
				out.append(contextTitle[i]+"2,");

			out.append("cosine_similarity,class\n");
			///////************

			while ((nextLine = reader.readNext()) != null) {

				double cosineSim = 0;

				int id1 = Integer.parseInt(nextLine[0]);
				int id2 = Integer.parseInt(nextLine[1]);

				line.append(id1+","+id2+",");

				for(int i=2;i<9;i++){
					double value = Double.parseDouble(nextLine[i]);
					value = Math.round(value*100)/100.0d;
					line.append(value+",");
				}

				Double[] context1 = contextMap.get(id1);
				Double[] context2 = contextMap.get(id2);

				for(int i=0;i<context1.length;i++)
					line.append(context1[i]+",");

				for(int i=0;i<context2.length;i++)
					line.append(context2[i]+",");

				cosineSim = cosineSimilarity(context1, context2);

				line.append(cosineSim+",");

				line.append(nextLine[9]+"\n");

				out.append(line);

				line = new StringBuffer();
			}
			out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			// TODO: handle exception
		}
	}

    public void createSuperJoinTable(String inputBmfFileName, String inputContextFileName1, String inputContextFileName2, String outputfilename){

        Map<Integer,Double[]> contextMap1 = new HashMap<Integer,Double[]>();
        Map<Integer,Double[]> contextMap2 = new HashMap<Integer,Double[]>();
        String[] contextTitle1 = {};
        String[] contextTitle2 = {};
        String[] bmf_title = {};

        try {
            //csv file containing data
            String strFile1 = inputContextFileName1;
            String strFile2 = inputContextFileName2;
            CSVReader reader1 = new CSVReader(new FileReader(strFile1));
            CSVReader reader2 = new CSVReader(new FileReader(strFile2));
            String [] nextLine;
            int lineNumber = 0;

            contextTitle1 = reader1.readNext();
            contextTitle2 = reader2.readNext();

            int contextLineConter = 0;
            while ((nextLine = reader1.readNext()) != null) {

                contextLineConter++;

                Double[] temp = new Double[nextLine.length-1];
                for(int i=1;i<nextLine.length;i++){
                    temp[i-1] = Double.parseDouble(nextLine[i]);
                    temp[i-1] = Math.round(temp[i-1]*100)/100.0d;
                }

                contextMap1.put(Integer.parseInt(nextLine[0]), temp);
            }
            System.out.println("context lines: "+contextLineConter);

            contextLineConter = 0;
            while ((nextLine = reader2.readNext()) != null) {

                contextLineConter++;

                Double[] temp = new Double[nextLine.length-1];
                for(int i=1;i<nextLine.length;i++){
                    temp[i-1] = Double.parseDouble(nextLine[i]);
                    temp[i-1] = Math.round(temp[i-1]*100)/100.0d;
                }

                contextMap2.put(Integer.parseInt(nextLine[0]), temp);
            }
            System.out.println("context lines: "+contextLineConter);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("kangoro");
            // TODO: handle exception
        }

        StringBuffer line = new StringBuffer();

        try {

            //csv file containing data
            String strFile = inputBmfFileName;
            CSVReader reader = new CSVReader(new FileReader(strFile));
            String [] nextLine;

            bmf_title = reader.readNext();

            /////************
            FileWriter fstream = new FileWriter(outputfilename);
            BufferedWriter out = new BufferedWriter(fstream);

            for(int i=0;i<bmf_title.length-1;i++)
                out.append(bmf_title[i]+",");

            for(int i=1;i<contextTitle1.length;i++)
                out.append(contextTitle1[i]+"1,");

            for(int i=1;i<contextTitle1.length;i++)
                out.append(contextTitle1[i]+"2,");

            for(int i=1;i<contextTitle2.length;i++)
                out.append(contextTitle2[i]+"1,");

            for(int i=1;i<contextTitle2.length;i++)
                out.append(contextTitle2[i]+"2,");


            out.append("cosine_similarity,class\n");
            ///////************

            while ((nextLine = reader.readNext()) != null) {

                double cosineSim = 0;

                int id1 = Integer.parseInt(nextLine[0]);
                int id2 = Integer.parseInt(nextLine[1]);

                line.append(id1+","+id2+",");

                for(int i=2;i<9;i++){
                    double value = Double.parseDouble(nextLine[i]);
                    value = Math.round(value*100)/100.0d;
                    line.append(value+",");
                }

                Double[] context1_1 = contextMap1.get(id1);
                Double[] context2_1 = contextMap1.get(id2);

                Double[] context1_2 = contextMap2.get(id1);
                Double[] context2_2 = contextMap2.get(id2);

                for(int i=0;i<context1_1.length;i++)
                    line.append(context1_1[i]+",");

                for(int i=0;i<context2_1.length;i++)
                    line.append(context2_1[i]+",");

                for(int i=0;i<context1_2.length;i++)
                    line.append(context1_2[i]+",");

                for(int i=0;i<context2_2.length;i++)
                    line.append(context2_2[i]+",");

                Double[] bug1_all_context = Arrays.copyOf(context1_1, context1_1.length + context1_2.length);
                System.arraycopy(context1_2, 0, bug1_all_context, context1_1.length, context1_2.length);

                Double[] bug2_all_context = Arrays.copyOf(context2_1, context2_1.length + context2_2.length);
                System.arraycopy(context2_2, 0, bug2_all_context, context2_1.length, context2_2.length);

                cosineSim = cosineSimilarity(bug1_all_context, bug2_all_context);

                line.append(cosineSim+",");

                line.append(nextLine[9]+"\n");

                out.append(line);

                line = new StringBuffer();
            }
            out.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
            // TODO: handle exception
        }
    }

    public void createNewStackSimilarityTable(String originalFile, String outputFileName, double dupRatio, boolean includeForbidden){

        Vector<Integer> forbidden = new Vector<Integer>(Arrays.asList(36886,36469,36468,36185,33997,33988,25384,25158,24818,24655,24534,24373,24076,23694,23591,22984,22589,22495,22477,22418,22325,22239,22154,21757,21754,21753,21752,21741,21740,21733,21722,21682,21602,21574,21485,21454,21453,21285,21207,21198,21175,21128,21127,21068,21067,20852,20839,20750,20664,20644,20593,20565,20554,20477,20466,20445,20384,20301,20290,20262,20136,20135,20123,19957,19953,19945,19944,19840,19830,19654,19653,19624,19623,19619,19583,19456,19445,19416,19415,19134,19082,19006,18984,18808,18774,18768,18759,18754,18735,18729,18708,18675,18657,18590,18588,18584,18524,18466,18398,18368,18339,18328,18307,18254,18190,18175,18109,18108,18044,18021,17770,17542,17464,17171,17079,17069,17062,16842,16488,16468,16393,16282,16236,16204,16000,14919,14856,14654,14414,13411,13395,13132,13067,13056,12596,12362,12160,12006,11921,11661,11658,11517,11495,11398,11358,11129,11055,10990,10801,10785,10768,10692,10309,10308,9977,9868,9861,9467,9425,9277,9276,9224,9132,9050,8871,8722,8474,8381,8335,8073,8070,8062,7504,7290,6250,6114,5997,5969,5968,5967,5934,5928,5922,5916,5888,5640,5619,5599));

        try{
            Map<String,Double> stackPairsMap = new HashMap<String,Double>();
            String record="";
            BufferedReader fileRead= new BufferedReader(new FileReader(originalFile));
            fileRead.readLine();    // Discard title
            while((record=fileRead.readLine())!=null) {
                String[] values = record.split(";");
                stackPairsMap.put(values[0]+"-"+values[1], Double.valueOf(values[2]));
            }

            // Create file
            FileWriter fstream = new FileWriter(outputFileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.append("Bug 1,Bug 2,Similarity,Class\n");

            Document doc1 = new Document("", "");
            Document doc2 = new Document("", "");
            StringBuffer line = new StringBuffer("");
            String separator = ",";
            int dupCount = 0;

            for(int i=0;i<corpus.getDocuments().size()-1;i++){

                for(int j=i+1;j<corpus.getDocuments().size();j++){

                    line = new StringBuffer();
                    doc1 = corpus.getDocuments().get(i);
                    doc2 = corpus.getDocuments().get(j);

                    if(!includeForbidden && (forbidden.contains(doc1.getBugID()) || forbidden.contains(doc2.getBugID())))
                        continue;

                    if(!isDuplicate(doc1,doc2))
                        continue;

                    Integer id1, id2;
                    if(stackPairsMap.containsKey(doc1.getBugID() + "-" + doc2.getBugID())) {
                        id1 = doc1.getBugID();
                        id2 = doc2.getBugID();
//                    } else if (stackPairsMap.containsKey(doc2.getBugID() + "-" + doc1.getBugID())) {
//                        id1 = doc2.getBugID();
//                        id2 = doc1.getBugID();
                    } else {
                        continue;
                    }

                    Double similarity = stackPairsMap.get(id1 + "-" + id2);

                    line.append(id1+separator);
                    line.append(id2+separator);
                    line.append(similarity+separator);
                    line.append("dup\n");
                    out.append(line);
                    dupCount++;
                }
            }

            Random rand = new Random();
            int totalCount = dupCount;

            while (totalCount < dupCount/dupRatio) {

                line = new StringBuffer();
                int random = rand.nextInt(corpus.getDocuments().size()-1);
                doc1 = corpus.getDocuments().get(random);
                doc2 = corpus.getDocuments().get(random+1);

                if(!includeForbidden && (forbidden.contains(doc1.getBugID()) || forbidden.contains(doc2.getBugID())))
                    continue;

                if(isDuplicate(doc1,doc2))
                    continue;

                Integer id1, id2;
                if(stackPairsMap.containsKey(doc1.getBugID() + "-" + doc2.getBugID())) {
                    id1 = doc1.getBugID();
                    id2 = doc2.getBugID();
//                } else if (stackPairsMap.containsKey(doc2.getBugID() + "-" + doc1.getBugID())) {
//                    id1 = doc2.getBugID();
//                    id2 = doc1.getBugID();
                } else {
                    continue;
                }

                Double similarity = stackPairsMap.get(id1 + "-" + id2);

                line.append(id1+separator);
                line.append(id2+separator);
                line.append(similarity+separator);
                line.append("non\n");
                out.append(line);
                totalCount++;
            }

            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }
	
	private double reciprocal_function(double d1, double d2){

		if(d1 == 0  && d2 == 0)
			return 0;

		return (double)(1.0/(1+Math.abs(d1-d2)));
	}

	public static void main(String[] args) {

		HashMap<String,Double> freeVariables = new HashMap<String, Double>(){{
			put("w1",1.163);
			put("w2",0.013);
			put("w3",2.285);
			put("w4",0.032);
			put("w5",0.772);
			put("w6",0.381);
			put("w7",2.427);
			put("wsumm_uni",3.0);
			put("wdesc_uni",1.0);
			put("bsumm_uni",0.5);
			put("bdesc_uni",1.0);
			put("k1_uni",2.0);
			put("k3_uni",0.0);
			put("wsumm_bi",3.0);
			put("wdesc_bi",1.0);
			put("bsumm_bi",0.5);
			put("bdesc_bi",1.0);
			put("k1_bi",2.0);
			put("k3_bi",0.0);
		}};

		//Vector<String> cosine_files = new Vector<String>();
		//cosine_files.add("cosine_rep.csv");
		//cosine_files.add("lda_sums.csv");

		Preprocessing prep = new Preprocessing();
		AndroidXmlParser AXP = new AndroidXmlParser();
		Corpus corpus = new Corpus(prep.process(AXP.getBugs(), false));
		
		/*System.out.println("shoru");
		for(int i=0;i<corpus.getDocuments().size();i++){
			
			if(corpus.getDocuments().get(i).getStatus().toLowerCase().equals("duplicate"))
				System.out.println(corpus.getDocuments().get(i).getBugID());
			
		}*/
		
		
		MachineLearnerTableGenerator mltg = new MachineLearnerTableGenerator(false, freeVariables, corpus);
        mltg.createNewStackSimilarityTable("features/stackTraces/stackSimilarity.csv","features/stackTraces/stackSimilarity_20_80.csv", 0.2, false);
//        mltg.createContextualPairTable("domainWords","features/domain_context_pairs_10_90_including_forbidden.csv",0.1,true);
//        mltg.createContextualPairTable("domainWords","features/domain_context_pairs_10_90_excluding_forbidden.csv",0.1,false);
//        mltg.createContextualPairTable("domainWords","features/domain_context_pairs_20_80_including_forbidden.csv",0.2,true);
//        mltg.createContextualPairTable("domainWords","features/domain_context_pairs_20_80_excluding_forbidden.csv",0.2,false);
//        mltg.createContextualPairTable("domainWords","features/domain_context_pairs_30_70_including_forbidden.csv",0.3,true);
//        mltg.createContextualPairTable("domainWords","features/domain_context_pairs_30_70_excluding_forbidden.csv",0.3,false);
		//mltg.createBMFandCategoryTable("filtered_dups.csv");
		//mltg.createContextTable("mozilla_architecture", "mozilla_architecture_context.csv");
		//mltg.createContextTable("NFR3", "mozilla_NFR_context.csv");
		//mltg.createContextTable("top100JunkWords", "mozilla_junk_context.csv");
		//mltg.createContextTable("openoffice_lda_topics", "openoffice_lda_context.csv");
//        mltg.createContextTable("androidDevWords", "features/androidDev/android_dev_context_features.csv", "WidgetsAndViews,Database,Events,Services,ActivitiesServices,UI,Animation,MediaPlayer,Camera,Sensors");
//        mltg.createContextTable("AndroidLabeledTopicsContext", "labeled_lda_features.csv", "3G,alarm,android_market,app,audio,battery,bluethooth,bluetooth,browser,calculator,calendar," +
//                "calling,camera,car,compass,contact,CPU,date,dialing,display,download,email,facebook,flash,font,google_earth," +
//                "google_latitude,google_map,google_navigation,google_translate,google_voice,GPS,gtalk,image,input,IPV6,keyboard," +
//                "language,location,lock,memory,message,network,notification,picassa,proxy,radio,region,ringtone,rSAP,screen_shot," +
//                "SD_card,search,setting,signal,SIM_card,synchronize,system,time,touchscreen,twitter,UI,upgrade,USB,video,voice_call," +
//                "voice_recognition,voicedialing,voicemail,VPN,wifi,youtube");
		
		//mltg.createJoinTable("random_filtered.csv", "android_labeled_context.csv", "android_labeled_filtered.csv");
		//mltg.createJoinTable("dataset.csv", "android_junk_context.csv", "android_junk_bmf_category.csv");
		//mltg.createJoinTable("dataset.csv", "android_lda_context.csv", "android_lda_bmf_category.csv");
		//mltg.createJoinTable("dataset.csv", "android_nfr_context.csv", "android_nfr_bmf_category.csv");
		//mltg.createJoinTable("dataset.csv", "android_labeled_context.csv", "android_labeled_bmf_category.csv");

//        Corpus corpus = new Corpus(prep.process(AXP.getBugs(), false));
//        MachineLearnerTableGenerator mltg = new MachineLearnerTableGenerator(false, freeVariables, corpus);
//
//        mltg.createBMFandCategoryTable("features/textualCategorical_10_90_including_forbidden.csv", 0.1, true);
//        mltg.createBMFandCategoryTable("features/textualCategorical_10_90_excluding_forbidden.csv", 0.1, false);
//        mltg.createJoinTable("features/textualCategorical_10_90_including_forbidden.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_android_dev_context_10_90_including_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_10_90_excluding_forbidden.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_android_dev_context_10_90_excluding_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_20_80_including_forbidden.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_android_dev_context_20_80_including_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_20_80_excluding_forbidden.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_android_dev_context_20_80_excluding_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_30_70_including_forbidden.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_android_dev_context_30_70_including_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_30_70_excluding_forbidden.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_android_dev_context_30_70_excluding_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_10_90_excluding_forbidden.csv", "features/domain_context_features.csv", "features/all_features_domain_context_10_90_excluding_forbidden.csv");
//        mltg.createBMFandCategoryTable("features/textualCategorical_30_70_including_forbidden.csv", 0.3, true);
//        mltg.createBMFandCategoryTable("features/textualCategorical_30_70_excluding_forbidden.csv", 0.3, false);
//        mltg.createJoinTable("features/textualCategorical_30_70_including_forbidden.csv", "features/domain_context_features.csv", "features/all_features_domain_context_30_70_including_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_30_70_excluding_forbidden.csv", "features/domain_context_features.csv", "features/all_features_domain_context_30_70_excluding_forbidden.csv");
//        mltg.createJoinTable("features/textualCategorical_excluding_forbidden.csv", "features/labeled_lda_features.csv", "features/all_features_labeled_lda_context_20_80_excluding_forbidden.csv");
//        mltg.createSuperJoinTable("features/textualCategorical_10_90_including_forbidden.csv", "features/labeled_lda_features.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_lda_and_android_dev_context_10_90_including_forbidden.csv");
//        mltg.createSuperJoinTable("features/textualCategorical_10_90_excluding_forbidden.csv", "features/labeled_lda_features.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_lda_and_android_dev_context_10_90_excluding_forbidden.csv");
//        mltg.createSuperJoinTable("features/textualCategorical_20_80_including_forbidden.csv", "features/labeled_lda_features.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_lda_and_android_dev_context_20_80_including_forbidden.csv");
//        mltg.createSuperJoinTable("features/textualCategorical_20_80_excluding_forbidden.csv", "features/labeled_lda_features.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_lda_and_android_dev_context_20_80_excluding_forbidden.csv");
//        mltg.createSuperJoinTable("features/textualCategorical_30_70_including_forbidden.csv", "features/labeled_lda_features.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_lda_and_android_dev_context_30_70_including_forbidden.csv");
//        mltg.createSuperJoinTable("features/textualCategorical_30_70_excluding_forbidden.csv", "features/labeled_lda_features.csv", "features/androidDev/android_dev_context_features.csv", "features/androidDev/all_features_lda_and_android_dev_context_30_70_excluding_forbidden.csv");
//        mltg.createContextualPairTableFromAllFeatures("features/androidDev/all_features_android_dev_context_10_90_excluding_forbidden.csv","features/androidDev/android_dev_context_10_90_excluding_forbidden.csv");
//        mltg.createContextualPairTableFromAllFeatures("features/androidDev/all_features_android_dev_context_10_90_including_forbidden.csv","features/androidDev/android_dev_context_10_90_including_forbidden.csv");
//        mltg.createContextualPairTableFromAllFeatures("features/androidDev/all_features_android_dev_context_20_80_excluding_forbidden.csv","features/androidDev/android_dev_context_20_80_excluding_forbidden.csv");
//        mltg.createContextualPairTableFromAllFeatures("features/androidDev/all_features_android_dev_context_20_80_including_forbidden.csv","features/androidDev/android_dev_context_20_80_including_forbidden.csv");
//        mltg.createContextualPairTableFromAllFeatures("features/androidDev/all_features_android_dev_context_30_70_excluding_forbidden.csv","features/androidDev/android_dev_context_30_70_excluding_forbidden.csv");
//        mltg.createContextualPairTableFromAllFeatures("features/androidDev/all_features_android_dev_context_30_70_including_forbidden.csv","features/androidDev/android_dev_context_30_70_including_forbidden.csv");
//        mltg.createNewContextualPairTable("junkWords","features/junk_context_features_10_90_including_forbidden.csv", 0.1, true);
//        mltg.createNewContextualPairTable("junkWords","features/junk_context_features_10_90_excluding_forbidden.csv", 0.1, false);
//        mltg.createNewContextualPairTable("junkWords","features/junk_context_features_20_80_including_forbidden.csv", 0.2, true);
//        mltg.createNewContextualPairTable("junkWords","features/junk_context_features_20_80_excluding_forbidden.csv", 0.2, false);
//        mltg.createNewContextualPairTable("junkWords","features/junk_context_features_30_70_including_forbidden.csv", 0.3, true);
//        mltg.createNewContextualPairTable("junkWords","features/junk_context_features_30_70_excluding_forbidden.csv", 0.3, false);
    }
}
