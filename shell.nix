{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.clojure
    pkgs.clojure-lsp
    pkgs.clj-kondo
  ];
}

