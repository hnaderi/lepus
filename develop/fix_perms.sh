#!/usr/bin/env sh

chmod 600 keys/*
chmod 644 keys/ca_certificate.pem
sudo chown -R 999:999 keys/server*
