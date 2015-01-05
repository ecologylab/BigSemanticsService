Deployment Instructions
=======================

1. Make sure that you have a Java SE runtime installed.

   If not, download the latest Java SE from
   http://www.oracle.com/technetwork/java/javase/downloads/index.html.

   Place it in $HOME/setup.

1.1 On RPM based machines, use the RPM package.
    - To install, use: # rpm -ivh <jre-rpm>
    - To upgrade, use: # rpm -Uvh <jre-rpm>
    - To remove, use: # rpm -e <package-name>

2. Make a dedicated folder for downloader: $HOME/downloader.

3. Copy Downloader.jar, dpool.properties, and run-downloader.sh to the
   destination machine under the dedicated folder.

4. Configure the downloader.controller_base_url property in dpool.properties.

5. Use run-downloader.sh to run the downloader.
 
