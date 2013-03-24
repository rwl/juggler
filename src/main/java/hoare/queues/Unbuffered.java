package hoare.queues;

import hoare.Queue;

public class Unbuffered extends Queue {

	private int waiting_pushes;
	private int waiting_pops;

	public Unbuffered(Class<?> type) {
		super(type);
	}

	@Override
	public boolean isBuffered() {
		return false;
	}

	@Override
	public boolean isUnbuffered() {
		return true;
	}

	@Override
	public boolean push() {
		return waiting_pops > 0;
	}

	@Override
	public boolean pop() {
		return waiting_pushes > 0;
	}

	@Override
    protected void reset_custom_state() {
        waiting_pushes = pushes.size();
        waiting_pops   = pops.size();
    }

	@Override
	protected void process() {
        Operation operation = operations.getLast();

        if (operation instanceof Push) {
          waiting_pushes += 1;

          for (Operation pop_operation : pops.clone()) {
            if (operation.getBlocking_once() != null
            		&& operation.getBlocking_once().equals(pop_operation.getBlocking_once())) {
              continue;
            }

            value, error = operation.receive(); {
              error = pop_operation.send(); {
                value;
              }

              waiting_pops -= 1;
              operations.remove(pop_operation);
              pops.remove(pop_operation);
              if (error != null) {
            	  throw new RollbackError();
              }
            }

            if (error == null || error.getMessage().equals("already performed")) {
              waiting_pushes -= 1;
              operations.pop();
              pushes.pop();
              break;
            }
          }
        } else {  // Pop
          waiting_pops += 1

          for (Operation push_operation : pushes.clone()) {
            if (operation.getBlocking_once() != null
            		&& operation.getBlocking_once().equals(push_operation.getBlocking_once())) {
              continue;
            }

            error = operation.send(); {
              value = null;

              v, error = push_operation.receive(); {
                value = v;
              }

              waiting_pushes -= 1;
              operations.remove(push_operation);
              pushes.remove(push_operation);
              if (error) {
            	  throw new RollbackError();
              }

              value;
            }

            if (error == null || error.getMessage().equals("already performed")) {
              waiting_pops -= 1;
              operations.pop();
              pops.pop();
              break;
            }
          }
        }
	}
}
