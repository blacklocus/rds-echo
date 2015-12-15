RDS Echo
========

A tool to simplify automated restore-from-snapshot operations in Amazon RDS.

To get a snapshot into a usable state must happen in at least two distinct stages: the **initial restore request**, and
**instance modification** once it is available. Then there's usually some form of **promotion** where you ask "Is
everyone ready for me to swap out the old with the new instance?" and then (in our case) update the CNAME to point at the fresh
instance. Finally, the old instance will be destroyed.

[![Build Status](https://travis-ci.org/blacklocus/rds-echo.svg)](https://travis-ci.org/blacklocus/rds-echo)



## Get It ##
RDS Echo is a command line tool.

Current stable release: [rds-echo v0.1.7](https://github.com/blacklocus/rds-echo/releases/tag/v0.1.7)

e.g.
```bash
sudo curl -o /usr/local/bin/rds-echo -L "https://github.com/blacklocus/rds-echo/releases/download/v0.1.7/rds-echo" && \
sudo chmod +x /usr/local/bin/rds-echo
```


## Use It ##
Help is included in the command-line tool. You'll need to configure the tool. `rds-echo sample-props` will drop a template
configuration file in the current directory. Edit this and fill in all the properties before attempting any further commands.

```
$ rds-echo <command>

RDS Echo may be configured by rdsecho.properties in the current working directory,
or exporting a fully-populated RDS_ECHO_OPTS environment variable.
Run 'rds-echo sample-props' or 'rds-echo sample-opts' to get a configuration template.

Valid commands correspond to Echo stages:

  sample-opts   Prints a template RDS_ECHO_OPTS variable which must be fully configured and then
                exported before any other RDS Echo command will function.

  sample-props  Drops a template rdsecho.properties into the current working directory, which must
                be fully configured before any other RDS Echo command will function.

  new           Creates a stage 'new' instance from a snapshot. This is usually the longest
                operation.

  modify        Modifies a stage 'new' instance with remaining settings that could not be applied
                on create and advances stage to 'modified'.

  reboot        Reboots a stage 'modified' instance so that all settings may take full effect and
                advances stage to 'rebooted'.

  promote       Promotes a stage 'rebooted' instance so that it becomes the active instance behind
                the specified CNAME and advances stage to 'promoted'. Any previously 'promoted'
                instances will be moved to stage 'forgotten'.

  retire        Retires a stage 'forgotten' instance (destroys it) and advances stage to 'retired'.


See the README for more details at https://github.com/blacklocus/rds-echo
```

You will need to set up AWS auth as per the
[DefaultAWSCredentialProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html).


### The stages ###
This is our story: On regular occasion, restore the latest production snapshot to a new development instance, promote
that instance to replace its former self, and then destroy the old instance.

Less the first configuration template command, RDS Echo fulfills this assuming story through a series of **stages**.
Each `rds-echo command` progresses an Echo-managed instance through these stages:

  - (non-existent) --`rds-echo new`-->     **new**
  - **new**        --`rds-echo modify`-->  **modified**
  - **modified**   --`rds-echo reboot`-->  **rebooted**
  - **rebooted**   --`rds-echo promote`--> **promoted**
    - This also results in any previously **promoted** instance advancing to **forgotten**
  - **forgotten**  --`rds-echo retire`-->  **retired**
    - A retired instance is in the process of being destroyed or will be very soon.

So in the straightforward case, each command is run in succession after the previous commands stabilize and leave the
DB instance in the "available" state.

All rds-echo state tracking metadata is stored as AWS resource tags on the RDS instance themselves



## Futures ##

Support asynchronous confirmation, particularly the promotion step. Perhaps send out an e-mail with a confirmation link
(and even a cancel link to invalidate the particular promotion request). The link sets a flag in the instance tags
to indicate who or whom confirmation was received from.

  - Can we do this without requiring a service? The link has to update something just by a user clicking it, i.e. a GET
    request that alters the state of some AWS resource.



### Why? ###

You want to have one or more production-like, but-not-actual-production databases for development, integration testing,
QA, or whatever. It can take a while (for us about 28 hours) just to restore the snapshot to a new instance, not to
mention the specific combination of RDS parameters that have to partially re-figured every time a restore is desired.
In doing this repeatedly, there are bound to be instances where human error picks the wrong value for a parameter,
and the process has to start all over again.

Then there's a number of required modifications that can only be performed after the instance has initialized.
Again, many parameters and repeatedly prone to human error.



## License ##

rds-echo is released under the terms of

[The MIT License (MIT)](http://opensource.org/licenses/MIT)

Copyright (c) 2015 BlackLocus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
