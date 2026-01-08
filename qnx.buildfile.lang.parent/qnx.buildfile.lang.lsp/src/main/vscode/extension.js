const path = require('path')
const vscode = require('vscode')
const { LanguageClient } = require('vscode-languageclient/node')

function activate(context) {
  const jarPath = context.asAbsolutePath(
    path.join('server', 'qnx.buildfile.lang.lsp-1.0.6-shaded.jar')
  )

  const client = new LanguageClient(
    'qnx-buildfile-lang',
    'QNX Buildfile Language Server',
    {
      command: 'java',
      args: ['-jar', jarPath]
    },
    {
      documentSelector: [{ scheme: 'file', language: 'qnx-buildfile-lang' }]
    }
  )

  context.subscriptions.push(client.start())
}

exports.activate = activate
