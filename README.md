## What's Dummy Factory?
Dummy Factory generates dummy data for debugging use. Please don't use this for a production use.

## What does Dummy Factory generate?

* Organizations
* Sites
* Pages
* Users
* Web Content Articles
* Documents

## Required environment
* Java 1.8 or above

## Usage
1. Download jar file from [here](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) and place it in ${liferay-home}/deploy folder. 
2. Start Liferay bundle and login as an administrator.
3. After the jar is properly installed, navigate to ```Application``` in the right pane and under Tool folder, Dummy Factory portlet will be found. Please place that on a page.
4. Now you are ready to create dummy data! Enjoy! 

## How can I compile Dummy Factory on my own?
1. Clone this repository to your local. Please make sure you've already installed Gradle 3.0 or above and blade tool that Liferay provides.
2. At the root directory, run ```gradle clean assemble``` then ```liferay.dummy.factory-x.x.x.jar``` will be created under ```/build/libs/``` directory.
3. To install onto your Liferay bundle, start up Liferay bundle on your local and run ```blade deploy```. Dummy Factory portlet will be deployed.

## Bug / Enhancement request
Please create a issue on this repository.