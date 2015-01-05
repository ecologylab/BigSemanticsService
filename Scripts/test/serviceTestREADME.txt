Author: Kade Keith

serviceTest.py prints the json response from BigSemanticsService for any number of example urls. To generate the set of all example urls in the wrapper
repository, use BigSemanticsWrapperRepository/repositoryCataloguer/src/repository_cataloguer/RepositoryCataloguer.java. I we do not get a good 
response from the server (a status code other than 200), the the script writes "error on url: " + the url to jsonServiceResponse.txt