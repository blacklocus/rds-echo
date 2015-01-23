RDS Echo
========
A tool to simplify automated restore-from-snapshot operations in Amazon RDS.

To get a snapshot into a usable state must happen in at least two distinct stages: the initial restore request, and
instance modification once it is available.


### Why? ###

You want to have one or more production-like, but-not-actual-production databases for development, integration testing,
QA, or whatever. It can take a while (for us about 28 hours) just to restore the snapshot to a new instance, not to
mention the specific combination of RDS parameters that have to partially re-figured every time a restore is desired.
In doing this repeatedly, there are bound to be instances where human error picks the wrong value for a parameter,
and the process has to start all over again.

Then there's a number of required modifications that can only be performed after the instance has initialized.
Again, many parameters and repeatedly prone to human error.