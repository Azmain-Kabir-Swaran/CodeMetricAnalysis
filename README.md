
# Code Metric Analysis Tool
## Finding the best method-level source code metric to understand change-proneness

```
.
├── lib
│   └── read-tse-1.0.0.jar
│
├── outputs_csv
│   ├── checkstyle.csv
│   └── hadoop.csv
│
├── source_jsons
│   ├── checkstyle
│   └── hadoop
│
├── src
│   ├── main/java/org/example
│   │   └── CodeMetricAnalysis.java
│   │
│   └── test/java/org/example
│       └── CodeMetricAnalysisTest.java
│
├── correlation_calculation.py
├── pom.xml
├── README.md
└── requirements.txt
```

## Steps to Run the Program
  1. [Introduction](#1-introduction)
  2. [Requirements](#2-requirements)
  3. [Setup \& Usage](#3-setup--usage)
  4. [References](#4-references)


### 1. Introduction

This project investigates the relationship between code metrics specifically size, McCabe cyclomatic complexity, and readability, and the change-proneness of Java methods utilizing CodeShovel's [[1]](#ref1) detailed change history within the Apache Hadoop project (https://github.com/apache/hadoop) which contains 73,733 files/methods and Checkstyle project (https://github.com/apache/hadoop) which contains 3,733 files/methods. A Java program was developed to extract these metrics from JSON files representing each method's change history, and calculate the number of revisions, while excluding non-content altering changes. The results are compiled into 2 CSV files for those 2 projects, which are then analyzed by a Python program to calculate correlation coefficients (Pearson, Spearman, and Kendall’s Tau) between the metrics and revision frequency. This analysis try to identify predictors of method change-proneness to inform future software maintenance efforts. The project, structured as a Maven project for straightforward build and dependency management.


### 2. Requirements

- IntelliJ IDEA Community Edition was used to develop the Java program along Maven build tool and openjdk-19.
- Python 3.8.15 was used in a conda environment.
- Need to install required Python libraries from [requirements.txt](requirements.txt).
- Preferred operating system: Linux/macOS/Windows-WSL/Windows.


### 3. Setup & Usage
#### 3.1. Java Program for method's Size, McCabe, Readability, and Number of Revisions calculation:

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 1:** *Clone or download the repository and open it from the IntelliJ. In the [pom.xml](pom.xml) file all the dependencies are already added. However, the following steps need to be completed to run the Java program and get the results.*

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 2:** *Go to the terminal and make sure that the current directory is the root directory of this project.*

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 3:** *A [custom jar](lib/read-tse-1.0.0.jar) file is needed to be installed for calculating the readability. This jar is inside the [lib](lib) folder. For installing this jar, use the following command in the terminal:*

```bash
mvn install:install-file -Dfile=lib/read-tse-1.0.0.jar -DgroupId=COM.EXAMPLE.ALLSMALL -DartifactId=read-tse -Dversion=1.0.0 -Dpackaging=jar
```

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 4:** *Now for building and generating a compilable jar file "maven-assembly-plugin" is already included in the [pom.xml](pom.xml) file along with other necessary dependencies. So, the following commands need to be run from the terminal:*

```bash
mvn clean compile assembly:single
```

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 5:** *If the previous command successfully runs, then a "target" folder will be generated and inside that folder a jar file (e.g., `CodeMetricAnalysis-1.0-SNAPSHOT-jar-with-dependencies.jar`) can be seen. Now, It is time to get the output as CSV format using this jar file but before that it is very important to check all the JSON files from 2 projects are in the correct directories. Therefore, they are stored in the [source_jsons](source_jsons) folder and it has 2 sub-folders called [checksyle](source_jsons/checkstyle) and [hadoop](source_jsons/hadoop). Inside these 2 sub-folders all the JSON files are stored which are containing detailed change history of Checkstyle and Hadoop projects. Moreover, the generated output will be saved in a folder named [outputs_csv](outputs_csv).*

##### ***It is recommended to delete all the files inside this [outputs_csv](outputs_csv) folder before running the following command so that the new outputs will be stored in an empty [outputs_csv](outputs_csv) folder and it will not overwrite the current files in that folder. On the other hand, to run the generated jar file, 2 arguments are required where the first one is the directory where the 2 projects folder are located ([source_jsons](source_jsons)) and second one is the directory where the generated outputs will be saved ([outputs_csv](outputs_csv)).***

```bash
java -jar 'target/CodeMetricAnalysis-1.0-SNAPSHOT-jar-with-dependencies.jar' 'source_jsons' 'outputs_csv'
```

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 6:** *If the previous command runs properly then 2 CSV files will be generated inside the [outputs_csv](outputs_csv) folder for 2 projects named [checkstyle.csv](outputs_csv/checkstyle.csv) (for Checkstyle project) and [hadoop.csv](outputs_csv/hadoop.csv) (for Hadoop project).*


#### 3.2. Python Script for correlation coefficient and P-values calculation from 2 CSV files generated from the Java program:

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 1:** *At first, open this repository using Visual Studio Code (or, Jupyter Notebook). In this project, Python 3.8.15 was used inside a conda environment (it can be also run without a conda environment). Following commands can be utilized in the terminal for running it using conda (conda have to be installed already):*

```bash
# Create a new conda environment
conda create --name YourEnvName python=3.8.15  # Change "YourEnvName" to your desired conda environment name.

# Activate the conda environment
conda activate YourEnvName  # Change "YourEnvName" to the name you set in the previous command.

# Install the required packages using pip
pip install -r requirements.txt

# Install the required packages using conda (NO NEED TO USE THE BELOW COMMAND IF THE PREVIOUS COMMAND INSTALLS EVERYTHING PROPERLY)
conda install --file requirements.txt
```

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 2:** *Now it is time to get the results for correlation coefficient (Pearson, Spearman, and Kendall) and P-values calculation for the [checkstyle.csv](outputs_csv/checkstyle.csv) and [hadoop.csv](outputs_csv/hadoop.csv) located in [outputs_csv](outputs_csv) folder. Now, run the `correlation_calculation.py` script from the root directory of this project to get the results:*

```bash
python correlation_calculation.py
```

##### **&nbsp;&nbsp;&nbsp;&nbsp;Step 3:** *The result should contain the correlation coefficient between `Size vs. Revisions`, `McCabe vs. Revisions`, and `Readability vs. Revisions` for both projects as shown in the below:*

```
Correlations coefficients for checkstyle:
  Size vs. Revisions:
    Pearson: Coefficient=0.4294, P-value=3.037e-167
    Spearman: Coefficient=0.2999, P-value=2.170e-78
    Kendall: Coefficient=0.2321, P-value=3.431e-76
  McCabe vs. Revisions:
    Pearson: Coefficient=0.4017, P-value=9.787e-145
    Spearman: Coefficient=0.3437, P-value=6.216e-104
    Kendall: Coefficient=0.2772, P-value=2.892e-99
  Readability vs. Revisions:
    Pearson: Coefficient=-0.1906, P-value=7.248e-32
    Spearman: Coefficient=-0.1569, P-value=5.448e-22
    Kendall: Coefficient=-0.1119, P-value=5.674e-22


Correlations coefficients for hadoop:
  Size vs. Revisions:
    Pearson: Coefficient=0.3503, P-value=0.000e+00
    Spearman: Coefficient=0.2389, P-value=0.000e+00
    Kendall: Coefficient=0.1928, P-value=0.000e+00
  McCabe vs. Revisions:
    Pearson: Coefficient=0.2793, P-value=0.000e+00
    Spearman: Coefficient=0.1954, P-value=0.000e+00
    Kendall: Coefficient=0.1659, P-value=0.000e+00
  Readability vs. Revisions:
    Pearson: Coefficient=-0.1465, P-value=0.000e+00
    Spearman: Coefficient=-0.1664, P-value=0.000e+00
    Kendall: Coefficient=-0.1233, P-value=0.000e+00
```

### 4. References

<a name="ref1"></a>
[1] Grund, F., Chowdhury, S. A., Bradley, N. C., Hall, B., & Holmes, R. (2021, May). CodeShovel: Constructing method-level source code histories. In 2021 IEEE/ACM 43rd International Conference on Software Engineering (ICSE) (pp. 1510-1522). IEEE.
