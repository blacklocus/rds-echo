RDS Echo
========
A tool to simplify automated restore-from-snapshot operations in Amazon RDS.

To get a snapshot into a usable state must happen in at least two distinct stages: the **initial restore request**, and
**instance modification** once it is available. Then there's usually some form of **promotion** where you ask "Is
everyone ready for me to swap out the old with the new instance?" and then update the CNAME to point at the fresh
instance.


## Get It ##

TODO distribution



## The Workflow ##
RDS Echo fulfills this story: On regular occasion, restore the latest production snapshot to a new development instance,
promote that instance to replace its former self, and then destroy the old instance.

This is accomplished through a series of stages, each of which is a command in this tool. Instances progress through the
stages is tracked by AWS resource tags on the instances themselves.

###


## Deployment ##

TODO is there a deployable involved? How do users approve?



### Why? ###

You want to have one or more production-like, but-not-actual-production databases for development, integration testing,
QA, or whatever. It can take a while (for us about 28 hours) just to restore the snapshot to a new instance, not to
mention the specific combination of RDS parameters that have to partially re-figured every time a restore is desired.
In doing this repeatedly, there are bound to be instances where human error picks the wrong value for a parameter,
and the process has to start all over again.

Then there's a number of required modifications that can only be performed after the instance has initialized.
Again, many parameters and repeatedly prone to human error.