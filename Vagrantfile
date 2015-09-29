# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"

  #
  # Define a new machine named as "master", this allows us to use this
  # name to assign this machine to the necessary ansible group, such
  # that vagrant provisioning will properly configure this system as
  # the Jenkins master server.
  #
  config.vm.define "master" do |master|
    master.vm.network "forwarded_port", guest: 8080, host: 8080,
      auto_correct: true
  end

  config.vm.provision "ansible" do |ansible|

    ansible.sudo = true
    ansible.playbook = "ansible/openzfs-jenkins-master.yml"
    ansible.groups = {
      "openzfs-jenkins-master" => ["master"]
    }

    #
    # To make iteration on changes easier, it's possible to store the
    # vault password in a file (e.g. ".vault-password") and then
    # use the "vault_password_file" option instead of the
    # "ask_vault_pass" option. This will prevent the provisioning step
    # from requiring the password to be input manually, as it'll read
    # the password from the file instead. Please be conscious of the
    # security implications of doing this, though.
    #
    # Also note, "ask_vault_pass" and "vault_password_file" options are
    # mutually exclusive. So, ensure only one of these options is
    # uncommented, otherwise vagrant provisioning will fail.
    #
    ansible.ask_vault_pass = true
    #ansible.vault_password_file = ".vault-password"
  end
end
