import { NextResponse } from 'next/server';

interface Feature {
  title: string;
  description: string;
  tasks: string[];
}

interface GenerateRequest {
  description: string;
}

interface GenerateResponse {
  features: Feature[];
}

// Simple AI-powered feature generation based on project description
function generateFeaturesFromDescription(description: string): Feature[] {
  const keywords = description.toLowerCase();
  const features: Feature[] = [];

  // Common feature patterns
  if (keywords.includes('task') || keywords.includes('todo') || keywords.includes('manage')) {
    features.push({
      title: 'Task Management System',
      description: 'Core task creation, editing, and organization functionality',
      tasks: [
        'Design task data model',
        'Implement CRUD operations for tasks',
        'Add task categorization and filtering',
        'Create task status tracking'
      ]
    });
  }

  if (keywords.includes('auth') || keywords.includes('login') || keywords.includes('user')) {
    features.push({
      title: 'Authentication & User Management',
      description: 'User authentication, profile management, and security features',
      tasks: [
        'Implement user registration flow',
        'Add login/logout functionality',
        'Create password reset mechanism',
        'Add session management'
      ]
    });
  }

  if (keywords.includes('chat') || keywords.includes('message') || keywords.includes('conversation')) {
    features.push({
      title: 'Messaging System',
      description: 'Real-time messaging and conversation handling',
      tasks: [
        'Design message data model',
        'Implement real-time message delivery',
        'Add message history persistence',
        'Create typing indicators'
      ]
    });
  }

  if (keywords.includes('ai') || keywords.includes('intelligent') || keywords.includes('smart')) {
    features.push({
      title: 'AI-Powered Features',
      description: 'Artificial intelligence integration for enhanced functionality',
      tasks: [
        'Integrate AI model API',
        'Implement prompt engineering',
        'Add response streaming',
        'Create AI configuration options'
      ]
    });
  }

  // Default features if none matched
  if (features.length === 0) {
    features.push(
      {
        title: 'Core Application Setup',
        description: 'Foundation structure and configuration',
        tasks: [
          'Set up project structure',
          'Configure build system',
          'Add basic navigation',
          'Implement error handling'
        ]
      },
      {
        title: 'User Interface',
        description: 'Main user interface components and screens',
        tasks: [
          'Design main layout',
          'Create navigation flow',
          'Implement responsive design',
          'Add loading states'
        ]
      },
      {
        title: 'Data Management',
        description: 'Data persistence and state management',
        tasks: [
          'Design data models',
          'Implement local storage',
          'Add data synchronization',
          'Create backup strategy'
        ]
      }
    );
  }

  return features;
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { description } = body as GenerateRequest;

    if (!description) {
      return NextResponse.json(
        { error: 'description is required' },
        { status: 400 }
      );
    }

    const features = generateFeaturesFromDescription(description);
    
    return NextResponse.json({ features });
  } catch (error: any) {
    console.error('Generate features error:', error);
    return NextResponse.json(
      { error: 'Failed to generate features', details: error.message },
      { status: 500 }
    );
  }
}