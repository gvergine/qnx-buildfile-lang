const path = require('path')
const vscode = require('vscode')
const { LanguageClient } = require('vscode-languageclient/node')

const SERVER_JAR = 'qnx.buildfile.lang.lsp-1.0.8-shaded.jar'
const CONFIG_SECTION = 'qnx-buildfile-lang'
const CONFIG_KEY = 'customValidatorJarPath'

/** @type {LanguageClient | undefined} */
let client

/**
 * Build the java command arguments, including -DcustomValidatorJar if configured.
 */
function buildServerArgs(context) {
  const jarPath = context.asAbsolutePath(path.join('server', SERVER_JAR))
  const config = vscode.workspace.getConfiguration(CONFIG_SECTION)
  const customJar = config.get(CONFIG_KEY, '').trim()

  const args = []
  if (customJar) {
    args.push('-DcustomValidatorJar=' + customJar)
  }
  args.push('-jar', jarPath)

  return args
}

/**
 * Create and start a new LanguageClient.
 */
function startClient(context) {
  const args = buildServerArgs(context)

  client = new LanguageClient(
    'qnx-buildfile-lang',
    'QNX Buildfile Language Server',
    {
      command: 'java',
      args: args
    },
    {
      documentSelector: [{ scheme: 'file', language: 'qnx-buildfile-lang' }]
    }
  )

  client.start()
}

/**
 * Stop the current client (if running) and start a fresh one.
 */
async function restartClient(context) {
  if (client) {
    try {
      await client.stop()
    } catch (e) {
      // Client may already be stopped, ignore
    }
    client = undefined
  }
  startClient(context)
}

function activate(context) {
  // Start the language server
  startClient(context)

  // Register the manual restart command
  context.subscriptions.push(
    vscode.commands.registerCommand('qnx-buildfile-lang.restartServer', () => {
      vscode.window.showInformationMessage('Restarting QNX Buildfile Language Server...')
      restartClient(context)
    })
  )

  // Auto-restart when the custom validator JAR setting changes
  context.subscriptions.push(
    vscode.workspace.onDidChangeConfiguration(event => {
      if (event.affectsConfiguration(CONFIG_SECTION + '.' + CONFIG_KEY)) {
        vscode.window.showInformationMessage(
          'Custom validator setting changed. Restarting QNX Buildfile Language Server...'
        )
        restartClient(context)
      }
    })
  )
}

function deactivate() {
  if (client) {
    return client.stop()
  }
}

exports.activate = activate
exports.deactivate = deactivate
