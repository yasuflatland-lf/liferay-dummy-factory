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

## Required environment
* Java 1.8 or above
* Liferay 7.0 GA1 / Liferay DXP SP1 or above

## Usage
1. Download jar file from [here](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) and place it in ${liferay-home}/deploy folder. 
2. Start Liferay bundle and login as an administrator.
3. After the jar is properly installed, navigate to ```Control Panel``` in the left pane and under ```Apps``` folder, Dummy Factory portlet will be found. Please place that on a page.
4. Now you are ready to create dummy data! Enjoy!

## How can I compile Dummy Factory on my own?
1. Clone this repository to your local. Please make sure you've already installed Gradle 3.0 or above and blade tool that Liferay provides.
2. At the root directory, run ```gradle clean assemble``` then ```liferay.dummy.factory-x.x.x.jar``` will be created under ```/build/libs/``` directory.
3. To install onto your Liferay bundle, startup Liferay bundle on your local and run ```blade deploy```. Dummy Factory portlet will be deployed.

## Bug / Enhancement request
Please create a issue on this repository.
