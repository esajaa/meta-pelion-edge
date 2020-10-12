DESCRIPTION = "Utilities used by the WigWag Relay"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1dece7821bf3fd70fe1309eaa37d52a2"

SRC_URI="\
  git://git@github.com/armPelionEdge/edge-utils.git;protocol=ssh;name=wwrelay \
  git://git@github.com/armPelionEdge/edgeos-shell-scripts.git;protocol=ssh;name=dss;destsuffix=git/dss \
  git://git@github.com/armPelionEdge/node-i2c.git;protocol=ssh;name=node_i2c;destsuffix=git/tempI2C/node-i2c \
  git://git@github.com/armPelionEdge/pe-utils.git;protocol=ssh;name=pe-utils;destsuffix=git/pe-utils \
  file://ld_preload.patch \
  file://wwrelay \
  file://BUILDMMU.txt \
  file://wwrelay.service \
  file://wait-for-pelion-identity.service \
  file://do-post-upgrade.service \
  file://logrotate_directives/ \
"

SRCREV_FORMAT = "wwrelay-dss"
SRCREV_wwrelay = "7020d6bd0d3a486299d33e20be3f11b61372ebeb"
SRCREV_dss = "04db833a43b80ecdfae07fd388bbe4e242771f38"
SRCREV_node_i2c = "511b1f0beae55bd9067537b199d52381f6ac3e01"
SRCREV_pe-utils = "a712d9f0f01bec9a9aa70dda7153cff2f2ba1f3f"

inherit pkgconfig gitpkgv update-rc.d systemd

INHIBIT_PACKAGE_STRIP = "1"

INITSCRIPT_NAME = "wwrelay"
INITSCRIPT_PARAMS = "defaults 80 20" 

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE_${PN} = "wait-for-pelion-identity.service wwrelay.service do-post-upgrade.service"
SYSTEMD_AUTO_ENABLE_${PN} = "enable"

PV = "1.0+git${SRCPV}"
PKGV = "1.0+git${GITPKGV}"
PR = "r7"

DEPENDS = "update-rc.d-native"
RDEPENDS_${PN} += " bash openssl "

FILES_${PN} = "\
  /wigwag/*\
  /wigwag/etc\
  /wigwag/etc/*\
  /etc/logrotate.d/*\
  /etc/init.d\
  /etc/init.d/*\
  /etc/wigwag\
  /etc/wigwag/*\
  /etc/rc?.d/*\
  /usr/bin\
  /usr/bin/*\
  /etc/*\
  /userdata\
  /upgrades\
  /localdata\
  ${systemd_system_unitdir}/wwrelay.service\
  ${systemd_system_unitdir}/wait-for-pelion-identity.service\
  ${systemd_system_unitdir}/do-post-upgrade.service\
"

S = "${WORKDIR}/git"
S_MODPROBED="${S}/etc/modprobe.d"
S_PROFILED="${S}/etc/profile.d"
SWSB="${S}/system/bin"
SDTB ="${S}/dev-tools/bin"

do_package_qa () {
	echo "done"
}

do_log(){
	echo -e "$1" >> /tmp/YOCTO_wwrelay-utils.log
}
do_configure(){
	echo "its a new build (erasing old log)" > /tmp/YOCTO_wwrelay-utils.log
}

do_compile() {
	BUILDMMU=$(cat ${S}/../BUILDMMU.txt)
	VER_FILE=${S}/version.json
	if [ -e $VER_FILE ] ; then
		rm $VER_FILE
	fi
	echo  "{" > $VER_FILE
	echo  "   "  \"version\" ":" \"0.0.1\", >> $VER_FILE
	echo  "   "  \"packages\" ":" [{ >> $VER_FILE
	echo  "      "  \"name\" ":" \"WigWag-Firmware\", >> $VER_FILE
	echo  "      "  \"version\" ":" \"${BUILDMMU}\", >> $VER_FILE
	echo  "      "  \"description\" ":" \"Base Factory deviceOS\", >> $VER_FILE
	echo  "      "  \"node_module_hash\" ":" \"\", >> $VER_FILE
	echo  "      "  \"ww_module_hash\" ":" \"\" >> $VER_FILE
	echo  "   "  }]  >> $VER_FILE
	echo  "}" >> $VER_FILE
}

do_dirInstall(){
	pushd . >> /dev/null
	cd $1
	find . -type d -exec install -d $2/{} \;
	find . -type f -exec install -m 0755 {} $2/{} \; 
  popd >> /dev/null
}



do_install() {
	#create all directoires
	install -d ${D}${INIT_D_DIR}
	install -d ${D}/localdata
	install -d ${D}/userdata
	install -d ${D}/upgrades
	install -d ${D}/etc
	install -d ${D}/etc/dnsmasq.d
	install -d ${D}/etc/profile.d
	install -d ${D}/etc/modprobe.d
	install -d ${D}/etc/network
	install -d ${D}/etc/udev
	install -d ${D}/etc/udev/rules.d
  install -d ${D}/wigwag/system/bin
	do_dirInstall ${S}/wigwag/ ${D}/wigwag/
	install -m 0755 ${S}/etc/dnsmasq.conf ${D}/etc/dnsmasq.conf
	install -m 0755 ${S}/etc/dnsmasq.d/dnsmasq.conf ${D}/etc/dnsmasq.d/dnsmasq.conf
	install -m 0755 ${S}/etc/modprobe.d/at24.conf ${D}/etc/modprobe.d/at24.conf
	install -m 0755 ${S}/etc/profile.d/wigwagpath.sh ${D}/etc/profile.d/wigwagpath.sh
	install -m 0755 ${WORKDIR}/wwrelay ${D}/wigwag/system/bin
	install -m 0755 ${S}/etc/init.d/devjssupport ${D}${INIT_D_DIR}
	install -m 0755 ${S}/etc/init.d/relayterm ${D}${INIT_D_DIR}
	install -m 0755 ${S}/etc/init.d/wwfunctions ${D}${INIT_D_DIR}

# Install systemd units
  install -d ${D}${systemd_system_unitdir}
  install -m 644 ${WORKDIR}/wwrelay.service ${D}${systemd_system_unitdir}/wwrelay.service
  install -m 644 ${WORKDIR}/wait-for-pelion-identity.service ${D}${systemd_system_unitdir}/wait-for-pelion-identity.service
  install -m 644 ${WORKDIR}/do-post-upgrade.service ${D}${systemd_system_unitdir}/do-post-upgrade.service

	#spreadsheet work needed
	#conf
	cp -r ${S}/conf ${D}/wigwag/wwrelay-utils/
	
	#version
	install -m 0755 ${S}/version.json ${D}/wigwag/wwrelay-utils/conf/versions.json
	install -m 0755 ${S}/version.json ${D}/wigwag/etc/versions.json
	
    install -m 0755 ${S}/initscripts/UDEV/96-local.rules ${D}/etc/udev/rules.d/96-local.rules

	mkdir -p ${D}${bindir}

	#populate the /wigwag/system/lib
	install -d ${D}/wigwag/log
	install -d ${D}/wigwag/devicejs/conf
	install -d ${D}/wigwag/etc
	install -d ${D}/wigwag/etc/devicejs
	install -d ${D}/wigwag/support
	install -d ${D}/wigwag/devicejs/devjs-usr/App
	cd ${S}/conf
	install -d ${D}${sysconfdir}/wigwag
	install -d ${D}/wigwag/devicejs-core-modules
	#install -d ${D}/wigwag/devicejs-core-modules/Runner
	install -d ${D}/wigwag/wigwag-core-modules/
	install -d ${D}/wigwag/wigwag-core-modules/relay-term/
	install -d ${D}/wigwag/wigwag-core-modules/relay-term/config/

	#logrotate.d
	install -d "${D}${sysconfdir}/logrotate.d/"
	ALL_LDs="$(ls ${WORKDIR}/logrotate_directives)"
	for f in $ALL_LDs; do
		install -m 644 "${WORKDIR}/logrotate_directives/$f" "${D}${sysconfdir}/logrotate.d"
	done 

    cp -r ${S}/pe-utils/identity-tools ${D}/wigwag/wwrelay-utils/
}



