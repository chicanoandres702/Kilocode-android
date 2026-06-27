/**
 * MCP Client for GitHub MCP server.
 * Connects via stdio JSON-RPC to @modelcontextprotocol/server-github.
 */

import { spawn, ChildProcess } from 'child_process';

interface McpTool {
  name: string;
  description: string;
  inputSchema: {
    type: string;
    properties: Record<string, { type: string; description?: string; enum?: string[]; items?: any; minimum?: number; maximum?: number }>;
    required?: string[];
    additionalProperties?: boolean;
  };
}

interface McpResult {
  result?: {
    tools?: McpTool[];
    content?: Array<{ type: string; text: string }>;
  };
  error?: { message: string; code?: number };
}

export class GitHubMcpClient {
  private process: ChildProcess | null = null;
  private initialized = false;
  private requestId = 0;
  private pendingRequests = new Map<number, { resolve: (value: any) => void; reject: (err: Error) => void }>();

  async initialize(): Promise<void> {
    if (this.initialized) return;

    const token = process.env.GITHUB_PERSONAL_ACCESS_TOKEN;
    if (!token) {
      throw new Error('GITHUB_PERSONAL_ACCESS_TOKEN is not set');
    }

    this.process = spawn('npx', ['-y', '@modelcontextprotocol/server-github'], {
      stdio: ['pipe', 'pipe', 'pipe'],
      env: {
        ...process.env,
        GITHUB_PERSONAL_ACCESS_TOKEN: token,
      },
    });

    // Read stdout line by line (each line is a JSON-RPC message)
    let buffer = '';
    this.process.stdout!.on('data', (data: Buffer) => {
      buffer += data.toString();
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      for (const line of lines) {
        if (!line.trim()) continue;
        try {
          const msg = JSON.parse(line);
          if (msg.id !== undefined && this.pendingRequests.has(msg.id)) {
            const { resolve, reject } = this.pendingRequests.get(msg.id)!;
            this.pendingRequests.delete(msg.id);
            if (msg.error) {
              reject(new Error(`${msg.error.message} (code: ${msg.error.code})`));
            } else {
              resolve(msg);
            }
          }
        } catch {
          // ignore malformed lines
        }
      }
    });

    this.process.stderr!.on('data', (data: Buffer) => {
      console.error('[GitHub MCP stderr]:', data.toString());
    });

    this.process.on('exit', (code) => {
      console.error(`[GitHub MCP] Process exited with code ${code}`);
      this.process = null;
      this.initialized = false;
    });

    // Send initialize request
    const initResult = await this.sendRequest('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: {},
      clientInfo: { name: 'kilo-android-backend', version: '1.0.0' },
    });

    if (initResult.error) {
      throw new Error(`MCP init failed: ${initResult.error.message}`);
    }

    // Send initialized notification
    this.sendNotification('notifications/initialized', {});

    this.initialized = true;
  }

  private sendRequest(method: string, params: any): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.process || !this.process.stdin) {
        reject(new Error('MCP process not running'));
        return;
      }
      const id = ++this.requestId;
      this.pendingRequests.set(id, { resolve, reject });
      const msg = JSON.stringify({ jsonrpc: '2.0', id, method, params });
      this.process.stdin.write(msg + '\n');

      // Timeout after 30s
      setTimeout(() => {
        if (this.pendingRequests.has(id)) {
          this.pendingRequests.delete(id);
          reject(new Error(`MCP request timeout: ${method}`));
        }
      }, 30000);
    });
  }

  private sendNotification(method: string, params: any): void {
    if (!this.process || !this.process.stdin) return;
    const msg = JSON.stringify({ jsonrpc: '2.0', method, params });
    this.process.stdin.write(msg + '\n');
  }

  async listTools(): Promise<McpTool[]> {
    await this.initialize();
    const result = await this.sendRequest('tools/list', {});
    return result.result?.tools || [];
  }

  async callTool(name: string, args: Record<string, any>): Promise<string> {
    await this.initialize();
    const result = await this.sendRequest('tools/call', { name, arguments: args });
    if (result.error) {
      throw new Error(`MCP tool call failed: ${result.error.message}`);
    }
    const content = result.result?.content;
    if (content && content.length > 0) {
      return content[0].text || '';
    }
    return '';
  }

  /**
   * Search GitHub repositories using the MCP tool.
   */
  async searchRepositories(query: string, perPage = 30): Promise<any[]> {
    const result = await this.callTool('search_repositories', {
      query,
      perPage,
    });
    try {
      return JSON.parse(result);
    } catch {
      return [];
    }
  }

  async destroy(): Promise<void> {
    if (this.process) {
      this.process.kill();
      this.process = null;
      this.initialized = false;
    }
  }
}

// Singleton instance
let clientInstance: GitHubMcpClient | null = null;

export function getGitHubMcpClient(): GitHubMcpClient {
  if (!clientInstance) {
    clientInstance = new GitHubMcpClient();
  }
  return clientInstance;
}
