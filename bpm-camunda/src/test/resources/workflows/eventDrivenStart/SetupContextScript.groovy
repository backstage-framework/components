package workflows.camundaEventDrivenStart

import com.backstage.bpm.domain.Process
import groovy.util.logging.Slf4j

@Slf4j
class SetupContextScript
{
	def execute(Process process, def description)
	{
		log.info("Переданный в скрипт параметр: $description.")

		process.parameters['resultParam'] = description
	}
}
