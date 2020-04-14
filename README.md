## What's Dummy Factory?
[![Build Status](https://travis-ci.org/yasuflatland-lf/liferay-dummy-factory.svg?branch=master)](https://travis-ci.org/yasuflatland-lf/liferay-dummy-factory)
[![Coverage Status](https://coveralls.io/repos/github/yasuflatland-lf/liferay-dummy-factory/badge.svg)](https://coveralls.io/github/yasuflatland-lf/liferay-dummy-factory)

Dummy Factory generates dummy data for debugging use. Please don't use this for a production use.

## What does Dummy Factory generate?

* Organizations
* Sites
* Pages
* Users
* Web Content Articles
* Documents
* Message Board (Threads / Categories)
* Category (Categories / Vocabularies)
* Wiki
* Blogs
* Company

## Required environment
* Java 1.8 or above
* Liferay 7.3 GA1 (Master / Develop branch)
* Liferay 7.2 GA1 / Liferay DXP 7.2 GA1 or above (Please see 7.2.x branch)
* Liferay 7.1 (Please see 7.1.x branch)
* Liferay 7.0 (Please see 7.0.x branch)

## Usage
| Version        | Link           | 
| ------------- |:------------- | 
| 7.3      | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) | 
| 7.2      | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest) | 
| 7.1      | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest) | 
| 7.0      | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest) | 

1. Download jar file according to the version above and place it int `${liferay-home}/deploy ` 
2. Start Liferay bundle and login as an administrator.
3. After the jar is properly installed, navigate to ```Control Panel``` in the left pane and under ```Apps``` folder, Dummy Factory portlet will be found. Please place that on a page.
4. Now you are ready to create dummy data! Enjoy!

## How can I compile Dummy Factory on my own?
1. Create a Liferay Workspace for appropriate version (7.2.x branch is for 7.2, for example)
2. Clone this repository to `${liferay_workspace_home}/modules`.
3. At the root directory, run `../gradlew clean assemble` or `blade gw assemble` if you've installed `blade` tool. ```liferay.dummy.factory-x.x.x.jar``` will be created under ```/build/libs/``` directory.
4. To install onto your Liferay bundle, startup Liferay bundle on your local and run ```blade deploy```. Dummy Factory portlet will be deployed.

## Bug / Enhancement request
Please create a issue on this repository.

## Known issues
- Wiki generator gets error and does not work due to denied access to JSON APIs before the initilization. Wiki looks like bing initialized at the first access to the Wiki Admin page in the control panel, right after spining up the Liferay bundle.
