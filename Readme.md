# Drammer GUI
This software is part of the open-source component of our paper "Drammer: Deterministic
Rowhammer Attacks on Mobile Devices", published in ACM Computer and
Communications Security (CCS) 2016. It allows you to test whether an Android
device is vulnerable to the Rowhammer bug. It does **not** allow you to root
your device.

This code base contains the source for our Android GUI app, which is a *wrapper around the native*, C/C++-based mobile Rowhammer
test implementation found [in this repo](https://github.com/vusec/drammer).

If you don't want to build the test yourself, we also provide a pre-built
[Android app](https://vvdveen.com/drammer/drammer.apk).

This app optionally collects basic statistics on the type of device and test
results so that we can gain insights into the number and type of vulnerable
devices in the wild, so please consider sharing them for science.

# Disclaimer 
If, for some weird reason, you think running this code broke your device, you
get to keep both pieces.

