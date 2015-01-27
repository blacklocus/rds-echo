RDS Echo
========
A tool to simplify automated restore-from-snapshot operations in Amazon RDS. TODO link to The Workflow section

To get a snapshot into a usable state must happen in at least two distinct stages: the **initial restore request**, and
**instance modification** once it is available. Then there's usually some form of **promotion** where you ask "Is
everyone ready for me to swap out the old with the new instance?" and then update the CNAME to point at the fresh
instance.



## Get It ##

TODO distribution



## Use It ##
Help is included in the command-line tool.

```
$ ./rds-echo

22:31:03 ERROR: Expected exactly one argument.
22:31:03 INFO : usage:
$ rds-echo <command>

RDS Echo is configured by rdsecho.properties in the current working directory.
 Run 'rds-echo config' to get a starter template.

Valid commands correspond to Echo stages:

  config  Drops an rdsecho.properties template into the current working directory, which must be
          configured before any other RDS Echo command will function.

  new     Creates a stage 'new' instance from a snapshot. This is usually the longest operation.

  modify  Modifies a stage 'new' instance with remaining settings that could not be applied on
          create and advances stage to 'modified'.

  reboot  Reboots a stage 'modified' instance so that all settings may take full effect and
          advances stage to 'rebooted'.

  promote Promotes a stage 'rebooted' instance so that it becomes the active instance behind the
          specified CNAME and advances stage to 'promoted'. Any previously 'promoted' instances
          will be moved to stage 'forgotten'.

  retire  Retires a stage 'forgotten' instance (destroys it) and advances stage to 'retired'.


See the README for more details at https://github.com/blacklocus/rds-echo
```

### The stages ###
This is our story: On regular occasion, restore the latest production snapshot to a new development instance, promote
that instance to replace its former self, and then destroy the old instance.

Less the first *config* command, RDS Echo fulfills this assuming story through a series of **stages**.
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